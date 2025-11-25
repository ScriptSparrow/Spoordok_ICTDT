import base64
import io
import logging
from PIL import Image

import requests
from typer import prompt

from config import Config

class OllamaConnector:
    def __init__(self, ollama_url: str):
        self.ollama_url = ollama_url
        self.logger = logging.getLogger(__name__)
        self.prompts = Config.Prompts

    def __ensure_model(self, model_name: str):
        """Ensure the specified model is available in Ollama."""
        # Implementation to check and load model if necessary
        try:
            # Check if model exists
            response = requests.get(
                f"{Config.OLLAMA_URL}/api/tags",
                timeout=5
            )
            
            if response.status_code == 200:
                models = response.json().get("models", [])
                model_names = [m.get("name", "") for m in models]
                
                if model_name in model_names:
                    self.logger.debug(f"Model '{model_name}' is already available")
                    return True
            
            # Model not found, pull it
            self.logger.info(f"Pulling model '{model_name}' from Ollama...")
            
            pull_response = requests.post(
                f"{Config.OLLAMA_URL}/api/pull",
                json={"name": model_name},
                stream=True,
                timeout=600  # 10 minutes for large models
            )
            
            if pull_response.status_code == 200:
                # Stream the progress
                for line in pull_response.iter_lines():
                    if line:
                        try:
                            status = line.decode('utf-8')
                            self.logger.debug(f"Pull status: {status}")
                        except:
                            pass
                
                self.logger.info(f"Successfully pulled model '{model_name}'")
                return True
            else:
                self.logger.error(f"Failed to pull model '{model_name}': {pull_response.status_code}")
                return False
                
        except requests.exceptions.RequestException as exc:
            self.logger.error(f"Failed to check/pull model '{model_name}': {exc}")
            return False
        

    def generate_caption(self, image_bytes: io.BytesIO, caption_prompt: str, model: str) -> str:
        """Generate a caption for the given image using Ollama."""
        # Implementation to call Ollama API for caption generation

        self.__ensure_model(model)

        image_bytes.seek(0)
        img = Image.open(image_bytes).convert("RGB")
        buffer = io.BytesIO()
        img.save(buffer, format="PNG")
        buffer.seek(0)
        image_b64 = base64.b64encode(buffer.read()).decode('utf-8')

        response = requests.post(
            f"{Config.OLLAMA_URL}/api/generate",
            json={
                "model": model,
                "prompt": caption_prompt,
                "images": [image_b64],
                "stream": False
            },
            timeout=60
        )

        self.logger.debug(f"Ollama response status: {response.status_code}")

        if response.status_code == 200:
            result = response.json()
            caption = result.get("text", "").strip()
            self.logger.debug(f"Generated caption: {caption}")
            return caption
        
        else:
            return "Failed to generate caption"
        
    def generate_caption_embedding(self, caption: str, model: str) -> list[float]:
        """Generate an embedding for the given caption using Ollama."""
        # Implementation to call Ollama API for embedding generation

        self.__ensure_model(model)

        response = requests.post(
            f"{Config.OLLAMA_URL}/api/embed",
            json={
                "model": model,
                "texts": caption]
            },
            timeout=60
        )

        self.logger.debug(f"Ollama embedding response status: {response.status_code}")

        if response.status_code == 200:
            result = response.json()
            embedding = result.get("embeddings", [])[0]
            self.logger.debug(f"Generated embedding of length {len(embedding)}")
            return embedding
        
        else:
            self.logger.error(f"Failed to generate embedding: {response.status_code}")
            return []