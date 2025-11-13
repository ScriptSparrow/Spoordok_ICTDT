import logging
from fastapi import APIRouter

logger = logging.getLogger(__name__)
router = APIRouter()

@router.get("/health")
async def health_check():
    """Health check endpoint"""
    logger.info("Health check performed")
    return {"status": "healthy"}