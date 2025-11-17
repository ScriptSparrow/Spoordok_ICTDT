# import io
# import logging
# import uuid
# from typing import Tuple, Dict, Any, List, Optional
# import base64
# import requests

# import torch
# from PIL import Image
# from transformers import CLIPProcessor, CLIPModel
# from huggingface_hub import snapshot_download
# from llama_index.vector_stores.elasticsearch import ElasticsearchStore
# from llama_index.core.schema import TextNode

# from config import Config

# # Keep existing docling imports (not used yet, but keep them)
# from docling.pipeline.vlm_pipeline import VlmPipeline
# from docling.document_converter import DocumentConverter, PdfFormatOption, ImageFormatOption
# from docling_core.types.doc.document import PictureDescriptionData
# from docling_core.types.io import DocumentStream
# from docling.datamodel.pipeline_options import PictureDescriptionVlmOptions
# from docling.datamodel.base_models import InputFormat
# from docling.datamodel.pipeline_options import VlmPipelineOptions
# from docling.datamodel import vlm_model_specs

# logger = logging.getLogger(__name__)
# logger.setLevel(Config.LogLevel)

# def __get_elastic_store(context_id: str) -> ElasticsearchStore:
#     """Get ElasticsearchStore for a specific context (conversation)."""
#     index_name = f"images-{context_id}"
#     return ElasticsearchStore(index_name=index_name, es_url=Config.ELASTICSEARCH_URL)

# # ---------------------------------------------------------------------
# # Image embedding helper (local HF model, configurable via Config)
# # ---------------------------------------------------------------------

# _clip_model: Optional[CLIPModel] = None
# _clip_processor: Optional[CLIPProcessor] = None
# _clip_device: Optional[torch.device] = None


# def _get_device() -> torch.device:
#     if Config.IMAGE_EMBED_DEVICE:
#         if Config.IMAGE_EMBED_DEVICE == "cuda" and torch.cuda.is_available():
#             return torch.device("cuda")
#         if Config.IMAGE_EMBED_DEVICE == "mps" and torch.backends.mps.is_available():
#             return torch.device("mps")
#         return torch.device("cpu")

#     # Auto-detect
#     if torch.cuda.is_available():
#         return torch.device("cuda")
#     if torch.backends.mps.is_available():
#         return torch.device("mps")
#     return torch.device("cpu")


# def _load_clip_model(model_type: Optional[str] = None) -> Tuple[CLIPModel, CLIPProcessor, torch.device]:
#     """Load the embedding model based on model_type from Config."""
#     global _clip_model, _clip_processor, _clip_device
    
#     # Get the actual model name using the config method
#     model_name = Config.get_embedding_model(model_type)
    
#     # If model already loaded and matches requested model, return cached
#     if (_clip_model is not None and _clip_processor is not None and 
#         _clip_device is not None and hasattr(_clip_model.config, '_name_or_path') and
#         _clip_model.config._name_or_path == model_name):
#         return _clip_model, _clip_processor, _clip_device

#     device = _get_device()
#     logger.info(f"Loading image embedding model '{model_name}' (type: {model_type or Config.EMBEDDING_MODEL_TYPE}) on device '{device}'")

#     snapshot_download(
#         repo_id=model_name,
#         ignore_patterns=["*.safetensors.idx", "*.h5"],
#         local_dir=Config.IMAGE_MODELS_PATH,
#         local_dir_use_symlinks=True,
#     )

#     _clip_model = CLIPModel.from_pretrained(model_name)
#     _clip_processor = CLIPProcessor.from_pretrained(model_name)
#     _clip_model.to(device)  # type: ignore[arg-type]
#     _clip_model.eval()
#     _clip_device = device
#     return _clip_model, _clip_processor, _clip_device


# def _compute_image_embedding(image_bytes: io.BytesIO, model_type: Optional[str] = None) -> Tuple[List[float], Dict[str, Any]]:
#     """Compute image embedding using specified model type."""
#     model, processor, device = _load_clip_model(model_type)
#     model_name = Config.get_embedding_model(model_type)

#     image_bytes.seek(0)
#     img = Image.open(image_bytes).convert("RGB")

#     inputs = processor(images=img, return_tensors="pt")  # type: ignore[call-arg]
#     inputs = {k: v.to(device) for k, v in inputs.items()}

#     with torch.no_grad():
#         image_features = model.get_image_features(**inputs)

#     image_features = image_features / image_features.norm(p=2, dim=-1, keepdim=True)
#     embedding_vec = image_features[0].cpu().tolist()

#     metadata: Dict[str, Any] = {
#         "model_name": model_name,
#         "model_type": model_type or Config.EMBEDDING_MODEL_TYPE,
#         "embedding_dim": len(embedding_vec),
#     }
#     return embedding_vec, metadata


# def embed_image(context_id: str, model_type: Optional[str], bytes: io.BytesIO) -> str:
#     """Embed image and generate initial caption for later retrieval."""
#     index_name = f"images-{context_id}"
    
#     try:
#         embedding, embed_meta = _compute_image_embedding(bytes, model_type)
#     except Exception as exc:
#         logger.exception("Failed to compute image embedding: %s", exc)
#         raise

#     # Generate quick caption at upload time
#     bytes.seek(0)
#     caption = _generate_image_caption(bytes)
    
#     store = __get_elastic_store(context_id)
#     doc_id = str(uuid.uuid4())

#     metadata = {
#         "type": "image",
#         "model_name": embed_meta["model_name"],
#         "model_type": embed_meta["model_type"],
#         "embedding_dim": embed_meta["embedding_dim"],
#         "caption": caption,  # Store for later retrieval
#         "image_bytes": base64.b64encode(bytes.getvalue()).decode('utf-8')  # Store actual image
#     }

#     node = TextNode(
#         id_=doc_id,
#         text=caption,  # Use caption as searchable text
#         metadata=metadata,
#         embedding=embedding,
#     )

#     try:
#         store.add(nodes=[node])
#     except Exception as exc:
#         logger.exception("Failed to store image embedding in Elasticsearch: %s", exc)
#         raise

#     logger.info(
#         "Stored image in index='%s' id='%s' caption='%s'",
#         index_name, doc_id, caption[:50]
#     )
#     return doc_id


# def _ensure_ollama_model(model_name: str) -> bool:
#     """Ensure an Ollama model is downloaded and available.
    
#     Args:
#         model_name: Name of the model (e.g., "llava:13b")
        
#     Returns:
#         True if model is available, False otherwise
#     """
#     try:
#         # Check if model exists
#         response = requests.get(
#             f"{Config.OLLAMA_URL}/api/tags",
#             timeout=5
#         )
        
#         if response.status_code == 200:
#             models = response.json().get("models", [])
#             model_names = [m.get("name", "") for m in models]
            
#             if model_name in model_names:
#                 logger.info(f"Model '{model_name}' is already available")
#                 return True
        
#         # Model not found, pull it
#         logger.info(f"Pulling model '{model_name}' from Ollama...")
        
#         pull_response = requests.post(
#             f"{Config.OLLAMA_URL}/api/pull",
#             json={"name": model_name},
#             stream=True,
#             timeout=600  # 10 minutes for large models
#         )
        
#         if pull_response.status_code == 200:
#             # Stream the progress
#             for line in pull_response.iter_lines():
#                 if line:
#                     try:
#                         status = line.decode('utf-8')
#                         logger.debug(f"Pull status: {status}")
#                     except:
#                         pass
            
#             logger.info(f"Successfully pulled model '{model_name}'")
#             return True
#         else:
#             logger.error(f"Failed to pull model '{model_name}': {pull_response.status_code}")
#             return False
            
#     except requests.exceptions.RequestException as exc:
#         logger.error(f"Failed to check/pull model '{model_name}': {exc}")
#         return False


# def _generate_image_caption(image_bytes: io.BytesIO) -> str:
#     """Generate a descriptive caption for an image using VLM."""
#     try:
#         model_name = Config.get_vlm_model("basic")
        
#         # Ensure model is available
#         if not _ensure_ollama_model(model_name):
#             logger.error(f"Model '{model_name}' not available for caption generation")
#             return "Caption failed - model not available"
        
#         image_bytes.seek(0)
#         img = Image.open(image_bytes).convert("RGB")
#         buffer = io.BytesIO()
#         img.save(buffer, format="PNG")
#         buffer.seek(0)
#         image_b64 = base64.b64encode(buffer.read()).decode('utf-8')
        
#         # Detailed caption prompt
#         response = requests.post(
#             f"{Config.OLLAMA_URL}/api/generate",
#             json={
#                 "model": model_name,
#                 "prompt": Config.Prompts.format_prompt("CAPTION_CREATION_PROMPT", "URBAN_PLANNER"),
#                 "images": [image_b64],
#                 "stream": False
#             },
#             timeout=60
#         )
        
#         logger.debug(f"Caption generation response status: {response.status_code}")

#         if response.status_code == 200:
#             caption = response.json().get("response", "").strip()
#             if caption and len(caption) > 20:
#                 return caption
#             return "Caption generation incomplete"
#         return f"Caption failed - HTTP {response.status_code}"
        
#     except Exception as exc:
#         logger.warning(f"Failed to generate caption: {exc}")
#         return "Caption generation error"


# def get_context_images(context_id: str, limit: int = 10) -> List[Dict[str, Any]]:
#     """Retrieve all image embeddings for a given context (conversation).
    
#     Args:
#         context_id: Unique identifier for the conversation/context
#         limit: Maximum number of images to retrieve
        
#     Returns:
#         List of dictionaries containing image metadata
#     """
#     store = __get_elastic_store(context_id)
    
#     try:
#         # Query elasticsearch directly for documents in this index
#         from elasticsearch import Elasticsearch
#         es = Elasticsearch([Config.ELASTICSEARCH_URL])
        
#         index_name = f"images-{context_id}"
        
#         # Check if index exists
#         if not es.indices.exists(index=index_name):
#             logger.info(f"No images found for context_id='{context_id}' (index does not exist)")
#             return []
        
#         # Search for all documents
#         result = es.search(
#             index=index_name,
#             body={
#                 "query": {"match_all": {}},
#                 "size": limit,
#                 "_source": ["metadata", "id"]
#             }
#         )
        
#         images = []
#         for hit in result["hits"]["hits"]:
#             images.append({
#                 "id": hit["_id"],
#                 "metadata": hit["_source"].get("metadata", {})
#             })
        
#         logger.info(f"Retrieved {len(images)} images for context_id='{context_id}'")
#         return images
        
#     except Exception as exc:
#         logger.exception(f"Failed to retrieve images for context_id='{context_id}': {exc}")
#         return []


# def analyze_context_images_with_vlm(
#     context_id: str,
#     prompt: str,
#     model_type: Optional[str] = None
# ) -> Dict[str, Any]:
#     """Analyze ALL images from a neighbourhood walk with aggregate insights.
    
#     Strategy:
#     1. Retrieve all images from Elasticsearch (including stored image bytes)
#     2. Send BOTH captions AND actual images to VLM for verification/analysis
#     3. VLM can verify captions and provide deeper insights using visual data
#     """
#     model_name = Config.get_vlm_model(model_type)
    
#     # Ensure model is available
#     if not _ensure_ollama_model(model_name):
#         return {"error": f"Model '{model_name}' is not available and failed to download"}
    
#     # Retrieve all images with captions AND image bytes from context
#     from elasticsearch import Elasticsearch
#     es = Elasticsearch([Config.ELASTICSEARCH_URL])
#     index_name = f"images-{context_id}"
    
#     if not es.indices.exists(index=index_name):
#         return {"error": "No images found for this context"}
    
#     # Get ALL images with their bytes (up to reasonable limit)
#     result = es.search(
#         index=index_name,
#         body={
#             "query": {"match_all": {}},
#             "size": 100,
#             "_source": ["metadata", "text"]
#         }
#     )
    
#     if not result["hits"]["hits"]:
#         return {"error": "No images found"}
    
#     # Build context from captions AND collect image bytes
#     image_summaries = []
#     image_b64_list = []
    
#     for idx, hit in enumerate(result["hits"]["hits"]):
#         caption = hit["_source"].get("text", "No caption")
#         image_summaries.append(f"Image {idx+1}: {caption}")
        
#         # Extract stored image bytes from metadata
#         metadata = hit["_source"].get("metadata", {})
#         image_b64 = metadata.get("image_bytes")
        
#         if image_b64:
#             image_b64_list.append(image_b64)
#         else:
#             logger.warning(f"Image {idx+1} has no stored bytes")
    
#     captions_text = "\n".join(image_summaries)
    
#     # Enhanced prompt that asks VLM to verify captions against actual images
#     aggregate_prompt = f"""You are analyzing {len(image_summaries)} images from a neighbourhood walk.

# CAPTIONS (to be verified against actual images):
# {captions_text}

# TASK:
# 1. Verify each caption against the actual image
# 2. Identify any inaccuracies or missed details in captions
# 3. Answer the following question using BOTH the captions AND your visual analysis:

# QUESTION: {prompt}

# Provide a comprehensive answer considering all visual information available."""

#     try:
#         # Send request with BOTH prompt AND all images
#         payload = {
#             "model": model_name,
#             "prompt": aggregate_prompt,
#             "stream": False
#         }
        
#         # Add images to payload (Ollama supports multiple images)
#         if image_b64_list:
#             payload["images"] = image_b64_list
#             logger.info(f"Sending {len(image_b64_list)} images to VLM for analysis")
#         else:
#             logger.warning("No image bytes found, proceeding with captions only")
        
#         response = requests.post(
#             f"{Config.OLLAMA_URL}/api/generate",
#             json=payload,
#             timeout=300  # Longer timeout for processing multiple images
#         )
        
#         if response.status_code != 200:
#             return {"error": f"VLM API error: {response.status_code}"}
        
#         analysis = response.json().get("response", "")
        
#         return {
#             "success": True,
#             "prompt": prompt,
#             "aggregate_analysis": analysis,
#             "images_analyzed": len(image_summaries),
#             "images_with_bytes": len(image_b64_list),
#             "model": model_name,
#             "context_id": context_id
#         }
        
#     except Exception as exc:
#         logger.exception(f"Failed aggregate analysis: {exc}")
#         return {"error": str(exc)}

# def get_captions(context_id: str) -> dict[str, str]:
#     """Retrieve captions of all images in a given context."""
#     store = __get_elastic_store(context_id)
    
#     try:
#         # Query elasticsearch directly for documents in this index
#         from elasticsearch import Elasticsearch
#         es = Elasticsearch([Config.ELASTICSEARCH_URL])
        
#         index_name = f"images-{context_id}"
        
#         # Check if index exists
#         if not es.indices.exists(index=index_name):
#             logger.info(f"No images found for context_id='{context_id}' (index does not exist)")
#             return {}
        
#         # Search for all documents
#         result = es.search(
#             index=index_name,
#             body={
#                 "query": {"match_all": {}},
#                 "size": 100,
#                 "_source": ["metadata", "id"]
#             }
#         )
        
#         captions = {}
#         for hit in result["hits"]["hits"]:
#             caption = hit["_source"].get("metadata", {}).get("caption", "")
#             image_id = hit["_source"].get("id", "")
#             captions[image_id] = caption
        
#         logger.info(f"Retrieved {len(captions)} captions for context_id='{context_id}'")
#         return captions
        
#     except Exception as exc:
#         logger.exception(f"Failed to retrieve captions for context_id='{context_id}': {exc}")
#         return {}