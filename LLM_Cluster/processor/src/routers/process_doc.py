
import io
import logging
from fastapi import APIRouter, HTTPException, UploadFile
from config import Config

from elastic.elastic import embed_document

router = APIRouter(prefix="/doc-processor")

from elasticsearch import Elasticsearch

logger = logging.getLogger(__name__)
logger.debug("Elasticsearch URL: %s", Config.ELASTICSEARCH_URL)
es = Elasticsearch(Config.ELASTICSEARCH_URL)

@router.post("/{index}/create-index")
async def create_index(index: str):
    """Create an index"""

    if es.indices.exists(index=index):
        raise HTTPException(status_code=400, detail="Index already exists")

    response = es.indices.create(index=index)
    if response.get("acknowledged") != True:
        raise HTTPException(status_code=500, detail="Failed to create index")

    logger.info(f"Index '{index}' created: {response}")

    return {
        "index_name": index,
        "status": "success",
        "message": f"Index '{index}' created successfully"
    }

@router.post("/{index}/index-image")
async def index_document(index: str, image: UploadFile):
    """Index a document (image metadata)"""

    if not es.indices.exists(index=index):
        raise HTTPException(status_code=404, detail="Index does not exist")
    
    content = await image.read()
    with io.BytesIO(content) as bytes_io: 
        embed_document(index_name=index, bytes=bytes_io)
    
    
