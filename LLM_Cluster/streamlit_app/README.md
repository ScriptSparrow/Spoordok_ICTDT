# Streamlit Testing Application

A Streamlit-based testing interface for the LLM Cluster document processing API.

## Features

- **Index Management**: Create, delete, and clean document indices
- **Image Upload**: Upload and process images with VLM models
- **Caption Viewing**: View generated captions for processed images
- **Model Information**: Check available VLM and embedding models
- **Context Description**: Query processed images with natural language

## Usage

### With Docker Compose (Recommended)

The Streamlit app is included in the docker-compose profiles:

```bash
# Run with testing profile (includes Streamlit)
docker-compose --profile testing up

# Run with full profile (all services including Streamlit)
docker-compose --profile full up
```

Access the application at: http://localhost:8501

### Standalone

```bash
cd streamlit_app
pip install -r requirements.txt
streamlit run app.py
```

Note: Make sure the processor API is running and accessible.

## API Configuration

The app is configured to connect to the processor service at `http://processor:8000` when running in Docker. For standalone usage, update the `API_BASE_URL` in `app.py`.

## Interface Overview

### Context Management (Sidebar)
- Enter a context ID to work with
- Create, delete, or clean indices
- All operations are scoped to the selected context

### Upload Image Tab
- Upload images (PNG, JPG, JPEG)
- Select VLM and embedding models
- Process images to generate captions and embeddings

### View Captions Tab
- View all captions stored in the current context
- Refresh to see newly added captions

### Models Tab
- View available VLM and embedding models
- Check what models are configured in the system

### Context Description Tab
- Query the context with natural language prompts
- Describe what you're looking for in the processed images

## Health Check

The expandable "API Health Check" section allows you to verify connectivity with the processor API.