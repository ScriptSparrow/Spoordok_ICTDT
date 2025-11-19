from fastapi import FastAPI
from fastapi.concurrency import asynccontextmanager
from fastapi.responses import RedirectResponse


from routers import health, process_doc, prompts_config



@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

app = FastAPI(
    title="LLM Cluster API", 
    version="1.0.0",
    lifespan=lifespan
    )



@app.get("/")
async def root():
    """Root endpoint"""
    return RedirectResponse(url="/docs", status_code=301)

app.include_router(health.router)
app.include_router(process_doc.router)
app.include_router(prompts_config.router)

