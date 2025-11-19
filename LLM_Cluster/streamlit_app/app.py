import streamlit as st
import requests
import io
from PIL import Image
from config import API_BASE_URL, DEFAULT_CONTEXT_ID, DEFAULT_VLM_MODEL, DEFAULT_EMBEDDING_MODEL, REQUEST_TIMEOUT

def display_api_error(error: requests.exceptions.RequestException, operation: str):
    """Display API error with detail if available"""
    error_msg = f"Error {operation}: "
    
    if hasattr(error, 'response') and error.response is not None:
        try:
            error_data = error.response.json()
            if 'detail' in error_data:
                error_msg += f"{error_data['detail']}"
            else:
                error_msg += f"HTTP {error.response.status_code}: {error}"
        except:
            error_msg += f"HTTP {error.response.status_code}: {error}"
    else:
        error_msg += str(error)
    
    st.error(error_msg)

st.set_page_config(
    page_title="LLM Cluster Testing",
    page_icon="🤖",
    layout="wide"
)

st.title("LLM Cluster Testing Interface")

# Sidebar for context selection
with st.sidebar:
    st.header("Context Management")
    context_id = st.text_input("Context ID", value=DEFAULT_CONTEXT_ID)
    
    # Display current API endpoint
    st.info(f"API: {API_BASE_URL}")
    
    col1, col2 = st.columns(2)
    with col1:
        if st.button("Create Index"):
            try:
                response = requests.post(
                    f"{API_BASE_URL}/doc-processor/{context_id}/index",
                    timeout=REQUEST_TIMEOUT
                )
                response.raise_for_status()
                st.success("Index created successfully!")
                st.json(response.json())
            except requests.exceptions.RequestException as e:
                display_api_error(e, "creating index")
    
    with col2:
        if st.button("Delete Index"):
            try:
                response = requests.delete(
                    f"{API_BASE_URL}/doc-processor/{context_id}/index",
                    timeout=REQUEST_TIMEOUT
                )
                response.raise_for_status()
                st.success("Index deleted successfully!")
                st.json(response.json())
            except requests.exceptions.RequestException as e:
                display_api_error(e, "deleting index")
    


# Main content
tab1, tab2, tab3, tab4 = st.tabs(["Upload Image", "View Captions", "Models", "Context Description"])

with tab1:
    st.header("Image Upload & Processing")
    uploaded_file = st.file_uploader("Choose an image", type=['png', 'jpg', 'jpeg'])
    
    if uploaded_file:
        st.image(uploaded_file, caption="Uploaded Image", width=300)
        
        # Model selection
        col1, col2 = st.columns(2)
        with col1:
            vlm_model = st.text_input("VLM Model", value=DEFAULT_VLM_MODEL)
        
        if st.button("Process Image"):
            with st.spinner("Processing image..."):
                try:
                    files = {"image": (uploaded_file.name, uploaded_file.getvalue(), uploaded_file.type)}
                    headers = {
                        "vlm-model-name": vlm_model,
                    }
                    
                    response = requests.post(
                        f"{API_BASE_URL}/doc-processor/{context_id}/index/image",
                        files=files,
                        headers=headers,
                        timeout=REQUEST_TIMEOUT
                    )
                    response.raise_for_status()
                    
                    result = response.json()
                    st.success("Image processed successfully!")
                    st.json(result)
                    
                except requests.exceptions.RequestException as e:
                    display_api_error(e, "processing image")

with tab2:
    st.header("Context Captions")
    
    col1, col2 = st.columns([1, 3])
    
    with col1:
        if st.button("Refresh Captions"):
            try:
                response = requests.get(
                    f"{API_BASE_URL}/doc-processor/{context_id}/index/captions",
                    timeout=REQUEST_TIMEOUT
                )
                response.raise_for_status()
                data = response.json()
                
                st.session_state.captions_data = data
                
            except requests.exceptions.RequestException as e:
                display_api_error(e, "fetching captions")
    
    with col2:
        if "captions_data" in st.session_state:
            data = st.session_state.captions_data
            st.metric("Caption Count", data['caption_count'])
            
            if data['captions']:
                st.subheader("Captions")
                for i, caption in enumerate(data['captions'], 1):
                    st.write(f"**{i}.** {caption}")
            else:
                st.info("No captions found in this context.")

with tab3:
    st.header("Available Models")
    
    if st.button("Fetch Models"):
        try:
            response = requests.get(
                f"{API_BASE_URL}/doc-processor/models",
                timeout=REQUEST_TIMEOUT
            )
            response.raise_for_status()
            models = response.json()
            
            col1, col2 = st.columns(2)
            
            with col1:
                st.subheader("VLM Models")
                if models.get('vlm_models'):
                    for model in models['vlm_models']:
                        st.write(f"• {model}")
                else:
                    st.info("No VLM models available")
            
            with col2:
                st.subheader("Embedding Models")
                if models.get('embedding_models'):
                    for model in models['embedding_models']:
                        st.write(f"• {model}")
                else:
                    st.info("No embedding models available")
                    
        except requests.exceptions.RequestException as e:
            display_api_error(e, "fetching models")

with tab4:
    st.header("Context Description")
    
    prompt = st.text_area(
        "Describe what you're looking for in this context:",
        placeholder="e.g., 'Find images containing cats and dogs'"
    )
    
    model_type = st.selectbox(
        "Model Type (optional)",
        ["Default", "Custom"],
        index=0
    )
    
    if st.button("Describe Context"):
        if prompt:
            try:
                payload = {
                    "prompt": prompt
                }
                if model_type != "Default":
                    payload["model_type"] = model_type.lower()
                
                response = requests.post(
                    f"{API_BASE_URL}/doc-processor/{context_id}/describe",
                    json=payload,
                    timeout=REQUEST_TIMEOUT
                )
                response.raise_for_status()
                
                result = response.json()
                st.success("Context description generated!")
                st.json(result)
                
            except requests.exceptions.RequestException as e:
                display_api_error(e, "describing context")
        else:
            st.warning("Please enter a prompt to describe the context.")

# Health check section
with st.expander("API Health Check"):
    if st.button("Check API Health"):
        try:
            response = requests.get(f"{API_BASE_URL}/health", timeout=REQUEST_TIMEOUT)
            response.raise_for_status()
            st.success("✅ API is healthy!")
            st.json(response.json())
        except requests.exceptions.RequestException as e:
            display_api_error(e, "checking API health")