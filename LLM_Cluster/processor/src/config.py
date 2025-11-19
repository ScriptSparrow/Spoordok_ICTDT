from enum import Enum
import os
import logging
from yaml import safe_load

class EmbeddingModelConfig:
    MODEL_NAME: str
    VECTOR_SIZE: int

    def __init__(self, model_name: str, vector_size: int) -> None:
        self.MODEL_NAME = model_name
        self.VECTOR_SIZE = vector_size 

class Prompts:
    PERSONAS: dict[str, str]
    PROMPTS: dict[str, str]

    def __init__(self) -> None:
        with open(os.path.join(os.path.dirname(__file__), "/config/prompts.yml"), "r") as f:
            data = safe_load(f)
        
        self.CAPTION_CREATION_PROMPT: str = data["prompts"]["CAPTION_CREATION_PROMPT"]
        self.PERSONAS: dict[str, str] = data.get("personas", {})
        self.PROMPTS: dict[str, str] = data.get("prompts", {})

    def format_prompt(self, prompt_name: str, persona_name: str) -> str:
        """Format a prompt by inserting the specified persona."""
        prompt_template = getattr(self, prompt_name, "")
        persona_text = self.PERSONAS.get(persona_name, "")
        prompt = prompt_template.replace("#{PERSONA}#", persona_text)
        
        return prompt
    
    def get_personas(self) -> list[str]:
        """Get a list of available persona names."""
        return list(self.PERSONAS.keys())

class Config:
    QDRANT_URL: str = os.environ.get("QDRANT_URL", "http://qdrant:9200")
    OLLAMA_URL: str = os.environ.get("OLLAMA_URL", "http://ollama:11434")
    IMAGE_EMBED_DEVICE: str | None = os.environ.get("IMAGE_EMBED_DEVICE", None)  # None = auto-detect
    IMAGE_MODELS_PATH: str = os.environ.get("IMAGE_MODELS_PATH", "/models")
    Prompts = Prompts()

    EMBEDDING_VECTOR_SIZE: int = 512

    DEFAULT_VLM_MODEL : str = "default"
    VLM_MODELS = {
        "default" : "qwen3-vl:2b",
        "default-mlx" : "qwen3-vl:4b-instruct-q8_0",
        "mlx-large" : "qwen3-vl:30b-a3b-instruct",

        "qwen-mlx-large": "qwen3-vl:30b-a3b",
        "qwen-mlx": "qwen2-vl:7b",
        "qwen-small": "qwen2-vl:2b",

        "moondream-small" : "moondream:1.8b",
    }


    REASONING_MODELS: dict[str, str] = {
        
    }

    DEFAULT_EMBEDDING_MODEL_TYPE: str = "gemma-embedding"
    EMBEDDING_MODELS : dict[str, EmbeddingModelConfig] = {

        "gemma-embedding" : EmbeddingModelConfig(
            model_name="embeddinggemma:300m",
            vector_size=768
        ),
        "nomic": EmbeddingModelConfig(
            model_name="nomic-embed-text:v1.5",
            vector_size=768 #Check this as Nomic has variable embeddings
        ),
    }

    @classmethod
    def get_vlm_model(cls, model_type: str | None = None) -> str:
        """Get VLM model name by type. Falls back to default if not specified or not found."""
        model_type = model_type or cls.DEFAULT_VLM_MODEL
        if model_type not in cls.VLM_MODELS:
            logger.warning(f"VLM model type '{model_type}' not found. Using default '{cls.DEFAULT_VLM_MODEL}'")
            model_type = cls.DEFAULT_VLM_MODEL
        return cls.VLM_MODELS[model_type]
    
    @classmethod
    def get_embedding_model(cls, model_type: str | None = None) -> EmbeddingModelConfig:
        """Get embedding model name by type. Falls back to default if not specified or not found."""
        model_type = model_type or cls.DEFAULT_EMBEDDING_MODEL_TYPE
        if model_type not in cls.EMBEDDING_MODELS:
            logger.warning(f"Embedding model type '{model_type}' not found. Using default '{cls.DEFAULT_EMBEDDING_MODEL_TYPE}'")
            model_type = cls.DEFAULT_EMBEDDING_MODEL_TYPE
        return cls.EMBEDDING_MODELS[model_type]
    
    LogLevel = logging.DEBUG

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


