import io
import logging
from fastapi import APIRouter, HTTPException, Header, UploadFile, Body
from pydantic import BaseModel
from typing import Optional
from vec_db.classes import ContextCreationResult
from llm_connector.ollama import OllamaConnector
from config import Config
from vec_db.vec_db import QDrantConnector

router = APIRouter(
    prefix="/doc-processor",
    tags=["Document Processing"]
    )

class DescribeRequest(BaseModel):
    prompt: str
    model_type: Optional[str] = None


ollama_connector = OllamaConnector(ollama_url=Config.OLLAMA_URL)
vector_db = QDrantConnector()
logger = logging.getLogger(__name__)

@router.get("/models")
async def get_models():
    """Get available models"""
    return {
        "vlm_models": Config.VLM_MODELS,
        "embedding_models": Config.EMBEDDING_MODELS
    }

@router.post("/{context_id}/index")
async def create_index(
    context_id: str,
    embedding_model:  str = Header(Config.DEFAULT_EMBEDDING_MODEL_TYPE, description="Embedding model to use")
    ):
    """Create an index"""
    model = Config.get_embedding_model(embedding_model)
    result = vector_db.create_context(context_id=context_id, embedding_size=model.VECTOR_SIZE)
    if result == ContextCreationResult.ALREADY_EXISTS:
        raise HTTPException(status_code=409, detail=f"Index '{context_id}' already exists")

    logger.info(f"Index '{context_id}' created")
    return {
        "index_name": context_id,
        "status": "success",
        "vector_size": model.VECTOR_SIZE,
        "message": f"Index '{context_id}' created successfully"
    }

@router.delete("/{context_id}/index")
async def delete_index(context_id: str):
    """Delete an index"""

    vector_db.delete_context(context_id=context_id)
    logger.info(f"Index '{context_id}' deleted")
    return {
        "index_name": context_id,
        "status": "success",
        "message": f"Index '{context_id}' deleted successfully"
    }

@router.get("/{context_id}/index/captions")
async def get_index_status(context_id: str):
    """Get status of captions in a context-specific index"""

    captions = vector_db.get_captions(context_id=context_id)
    logger.info(f"Retrieved {len(captions)} captions for context '{context_id}'")
    return {
        "context_id": context_id,
        "caption_count": len(captions),
        "captions": captions
    }

@router.post("/{context_id}/index/image")
async def index_document(
        context_id: str, 
        image: UploadFile,
        vlm_model_name: str = Header(Config.DEFAULT_VLM_MODEL , description="VLM model to use"),
        embedding_model:  str = Header(Config.DEFAULT_EMBEDDING_MODEL_TYPE, description="Embedding model to use")
    ):
    """Index an image into a context-specific index"""
    try:
        content = await image.read()

        caption_prompt = Config.Prompts.format_prompt(
            "CAPTION_CREATION_PROMPT",
            "CAPTION_GENERATOR"
        )

        with io.BytesIO(content) as bytes_io: 
            vlm_model = Config.get_vlm_model(vlm_model_name)
            caption = ollama_connector.generate_caption(bytes_io, caption_prompt=caption_prompt, model=vlm_model)

            embedding_model_config = Config.get_embedding_model()
            caption_embedding = ollama_connector.generate_caption_embedding(caption, model=embedding_model_config.MODEL_NAME)

            doc_id = vector_db.store_image_with_caption(
                context_id=context_id,
                caption=caption,
                caption_embedding=caption_embedding,
                metadata={
                    "filename": image.filename,
                    "content_type": image.content_type
                }
            )
            
        return {
            "status": "success", 
            "message": f"Image indexed in context '{context_id}'",
            "image_id": doc_id
        }
    
    except Exception as exc:
        logger.exception(f"Failed to index image: {exc}")
        raise HTTPException(status_code=500, detail=f"Failed to index image: {str(exc)}")


@router.post("/{context_id}/describe")
async def describe_context(context_id: str, request: DescribeRequest):
    pass # Implementation of describe_context endpoint goes here


@router.get("/{context_id}/images")
async def list_context_images(context_id: str, limit: int = 100):
    pass

