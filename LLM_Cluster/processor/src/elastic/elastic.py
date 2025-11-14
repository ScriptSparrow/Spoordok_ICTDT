import io
import os
from config import Config
from huggingface_hub import snapshot_download
from llama_index.vector_stores.elasticsearch import ElasticsearchStore
from docling.pipeline.vlm_pipeline import VlmPipeline
from docling.document_converter import DocumentConverter, PdfFormatOption, ImageFormatOption
from docling_core.types.io import DocumentStream
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import (
    VlmPipelineOptions,
)
from docling.datamodel import vlm_model_specs

models_path = "/models" 

def __get_elastic_store(index_name: str) -> ElasticsearchStore:
    return ElasticsearchStore(index_name=index_name, es_url=Config.ELASTICSEARCH_URL)

def __get_model(repo_id: str):
    """Download and cache the specified model from Hugging Face Hub."""
    
    try:
        if not os.path.exists(models_path):
            print(f"Downloading model '{repo_id}' to '{models_path}'")
            snapshot_download(repo_id=repo_id, local_dir=models_path)
        else:
            print(f"Model '{repo_id}' already cached at '{models_path}'")
    
        return models_path

    except Exception as e:
        print(f"Error downloading model '{repo_id}': {e}")
        return None

def __setup_model_cache() -> None:
    """Setup the model cache directory for Docling models."""
    # Create cache directory if it doesn't exist
    os.makedirs(models_path, exist_ok=True)
    
    # Check permissions
    print(f"Models path exists: {os.path.exists(models_path)}")
    print(f"Models path is writable: {os.access(models_path, os.W_OK)}")
    print(f"Models path contents: {os.listdir(models_path) if os.path.exists(models_path) else 'Not found'}")
    
    # Set Hugging Face cache directory (used by transformers and docling)
    os.environ["HF_HOME"] = models_path
    os.environ["TRANSFORMERS_CACHE"] = models_path
    os.environ["HF_HUB_CACHE"] = models_path
    
    # Set torch cache directory (for PyTorch models)
    os.environ["TORCH_HOME"] = models_path
    
    print(f"Model cache set to: {models_path}")
    print(f"HF_HOME: {os.environ.get('HF_HOME')}")

    for model_key, model_spec in Config.EMBEDDING_MODELS.items():
        print(f"Setting up model cache for '{model_key}' with spec: {model_spec}")
        __get_model(model_spec.repo_id)

def ensure_setup() -> None: 
    """Ensure that the Elasticsearch store is set up and model cache is configured."""
    __setup_model_cache()

def embed_document(index_name: str, model_type: str, bytes: io.BytesIO) -> None:
    """Embed a document into the specified Elasticsearch index."""
    store = __get_elastic_store(index_name=index_name)

    print(f"Setting up pipeline with artifacts_path: {models_path}")
    
    model_spec = Config.EMBEDDING_MODELS.get(model_type)
    if not model_spec:
        raise ValueError(f"Unsupported model type: {model_type}")

    pipeline_options = VlmPipelineOptions(
        vlm_options=model_spec,
        artifacts_path=models_path,
    )

    try:
        converter = DocumentConverter(
            format_options={
                InputFormat.PDF: PdfFormatOption(
                    pipeline_cls=VlmPipeline,
                    pipeline_options=pipeline_options,
                ),
                InputFormat.IMAGE: ImageFormatOption(
                    pipeline_cls=VlmPipeline,
                    pipeline_options=pipeline_options,
                )
            }
        )

        doc_stream = DocumentStream(name="input_document", stream=bytes)
        doc = converter.convert(source=doc_stream)
        
        # TODO: Complete the embedding logic here
        
    except Exception as e:
        print(f"Error during document conversion: {e}")
        print(f"Models directory after error: {os.listdir(models_path) if os.path.exists(models_path) else 'Not found'}")
        raise