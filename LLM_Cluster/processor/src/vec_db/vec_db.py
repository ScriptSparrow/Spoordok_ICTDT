from qdrant_client import QdrantClient
from qdrant_client.models import (
    VectorParams, 
    Distance, 
    PointStruct, 
    Filter, 
    FieldCondition, 
    Range,
    NamedVector,
    PayloadSchemaType
)
from config import Config
from typing import List, Dict, Optional
from uuid import uuid4

class QDrantConnector:

    def __init__(self):
        self.client = QdrantClient(
            url=Config.QDRANT_URL,
            api_key=None
        )

        self.vector_size = Config.VECTOR_SIZE or 512


    def create_context(self, context_id: str):
        """Create a collection with named vectors for multi-modal search."""
        self.client.create_collection(
            collection_name=context_id,
            vectors_config={
                "image": VectorParams(
                    size=self.vector_size,
                    distance=Distance.COSINE
                ),
                "caption": VectorParams(
                    size=self.vector_size,
                    distance=Distance.COSINE
                )
            }
        )
        
        # Create indexes for efficient filtering
        self.client.create_payload_index(
            collection_name=context_id,
            field_name="sequence_number",
            field_schema=PayloadSchemaType.INTEGER
        )
        
    def delete_context(self, context_id: str):
        """Delete a context (collection) in QDrant."""
        self.client.delete_collection(collection_name=context_id)

    def clean_context(self, context_id: str):
        """Clean (delete all points) in a context (collection) in QDrant."""
        # Need to recreate with the same multi-modal config
        self.client.delete_collection(collection_name=context_id)
        self.create_context(context_id)

    def store_image_with_caption(
        self, 
        context_id: str, 
        caption: str,
        caption_embedding: List[float],
        metadata: Dict
    ) -> str:
        """
        Store image with both visual and textual embeddings.
        
        Args:
            context_id: Collection name (e.g., 'walk_session_123')
            image_embedding: Visual embedding from CLIP/vision model
            caption: VLM-generated caption
            caption_embedding: Text embedding of caption
            metadata: {
                'image_path': str,
                'timestamp': str,
                'sequence_number': int,
                'location': Optional[Dict], # GPS coords if available
                'objects_detected': List[str], # e.g., ['tree', 'building']
            }
        """
        point_id = str(uuid4())
        
        # Store both embeddings using named vectors
        self.client.upsert(
            collection_name=context_id,
            points=[
                PointStruct(
                    id=point_id,
                    vector={
                        "caption": caption_embedding
                    },
                    payload={
                        "caption": caption,
                        "type": "neighborhood_image",
                        **metadata
                    }
                )
            ]
        )
        return point_id

    def query_by_text(
        self, 
        context_id: str, 
        query_embedding: List[float],
        limit: int = 5,
        using_vector: str = "caption"
    ):
        """
        Query images using text-based search.
        
        Args:
            using_vector: 'caption' for text search, 'image' for visual similarity
        """
        results = self.client.search(
            collection_name=context_id,
            query_vector=NamedVector(
                name=using_vector,
                vector=query_embedding
            ),
            limit=limit,
            with_payload=True
        )
        return results

    def query_sequential_context(
        self,
        context_id: str,
        point_id: str,
        window_size: int = 3
    ):
        """
        Get images before and after a specific point (temporal context).
        Useful for: "Show me what came before/after this scene"
        """
        points = self.client.retrieve(
            collection_name=context_id,
            ids=[point_id],
            with_payload=True
        )
        
        if not points or len(points) == 0 or points[0].payload is None:
            return []
            
        
        seq_num = points[0].payload.get("sequence_number", None)
        if seq_num is None:
            return []
        
        # Query by sequence range using proper Filter model
        results, _ = self.client.scroll(
            collection_name=context_id,
            scroll_filter=Filter(
                must=[
                    FieldCondition(
                        key="sequence_number",
                        range=Range(
                            gte=seq_num - window_size,
                            lte=seq_num + window_size
                        )
                    )
                ]
            ),
            with_payload=True,
            limit=window_size * 2 + 1
        )
        return results

    def get_captions(self, context_id: str) -> List[str]:
        """Retrieve all captions in a context."""
        results = self.client.scroll(
            collection_name=context_id,
            with_payload=True,
        ) 
        captions = []
        for point in results[0]:
            if point.payload and "caption" in point.payload:
                captions.append(point.payload["caption"])
        return captions
