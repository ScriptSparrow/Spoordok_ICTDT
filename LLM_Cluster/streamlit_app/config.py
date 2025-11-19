import os
from sys import platform

# API Configuration
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8000")

# Default values
DEFAULT_CONTEXT_ID = "test-context"
DEFAULT_VLM_MODEL = "default"
if os.getenv("PLATFORM_TYPE") == "macos":
    DEFAULT_VLM_MODEL = "default-mlx"  # Use MLX-optimized model on macOS

DEFAULT_EMBEDDING_MODEL = "gemma-embedding"

# Request timeout
REQUEST_TIMEOUT = 120