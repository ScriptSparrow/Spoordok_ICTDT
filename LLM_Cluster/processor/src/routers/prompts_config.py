



import io
import logging
from fastapi import APIRouter, HTTPException, UploadFile, Body
from pydantic import BaseModel
from typing import Optional
from config import Config

router = APIRouter(
    prefix="/prompts",
    tags=["Prompts Config"]
    )


@router.get("/personas")
async def get_personas():
    """Get available personas"""
    personas = Config.Prompts.get_personas()
    return {
        "personas": personas
    }
