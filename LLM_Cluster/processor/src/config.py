import os
from llama_index.embeddings.ollama import OllamaEmbedding
from docling.datamodel import vlm_model_specs

class Config:
    ELASTICSEARCH_URL: str = os.environ.get("ELASTICSEARCH_URL", "http://processor-elastic-db:9200")
    OLLAMA_URL: str = os.environ.get("OLLAMA_URL", "http://ollama:11434")

    EMBEDDING_MODELS = {
        "basic" : vlm_model_specs.SMOLDOCLING_TRANSFORMERS,
    }

class MacConfig(Config):

    EMBEDDING_MODELS = {
        "basic" : vlm_model_specs.SMOLDOCLING_MLX
    }

if os.environ.get("ENVIRONMENT") == "MAC":
    print("Using MacConfig")
    Config = MacConfig