# Base image
FROM python:3.10-slim AS base

# Set working directory
WORKDIR /app

# Copy requirements file
COPY requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY ./src/* .

# ============================================
# Release target (production without debugging)
# ============================================
FROM base AS release

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

# ============================================
# Debug target (with debugpy for remote debugging)
# ============================================
FROM base AS debug
RUN pip install --no-cache-dir debugpy
EXPOSE 8000
EXPOSE 5678
CMD ["python", "-m", "debugpy", "--listen", "0.0.0.0:5678", "--wait-for-client", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]