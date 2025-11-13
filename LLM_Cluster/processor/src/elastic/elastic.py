import io
from config import Config
from llama_index.vector_stores.elasticsearch import ElasticsearchStore
from docling.pipeline.vlm_pipeline import VlmPipeline
from docling.document_converter import DocumentConverter, PdfFormatOption, ImageFormatOption
from docling_core.types.io import DocumentStream
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import (
    VlmPipelineOptions,
)
from docling.datamodel import vlm_model_specs

def __get_elastic_store(index_name: str) -> ElasticsearchStore:
    return ElasticsearchStore(index_name=index_name, es_url=Config.ELASTICSEARCH_URL)



def embed_document(index_name: str, bytes: io.BytesIO) -> None:
    """Embed a document into the specified Elasticsearch index."""
    store = __get_elastic_store(index_name=index_name)


    pipeline_options = VlmPipelineOptions(
        vlm_options=vlm_model_specs.SMOLDOCLING_TRANSFORMERS
    )

    converter = DocumentConverter(
        format_options={
            InputFormat.PDF: PdfFormatOption(
                pipeline_cls=VlmPipeline,
                pipeline_options=pipeline_options,
            ),
            InputFormat.IMAGE: ImageFormatOption(
                pipeline_cls=VlmPipeline,
                pipeline_options=pipeline_options,
            )
        }
    )

    doc_stream = DocumentStream(name="input_document", stream=bytes)
    doc = converter.convert(source=doc_stream)
    



