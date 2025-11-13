from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional
from fastapi.responses import RedirectResponse
import uvicorn

from routers import health, process_doc

app = FastAPI(title="LLM Cluster API", version="1.0.0")


@app.get("/")
async def root():
    """Root endpoint"""
    return RedirectResponse(url="/docs", status_code=301)

app.include_router(health.router)
app.include_router(process_doc.router)


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
