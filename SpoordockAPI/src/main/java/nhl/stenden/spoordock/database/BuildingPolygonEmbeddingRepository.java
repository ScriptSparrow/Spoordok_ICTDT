package nhl.stenden.spoordock.database;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import jakarta.transaction.Transactional;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEmbeddingEntity;

public interface BuildingPolygonEmbeddingRepository extends ListCrudRepository<BuildingPolygonEmbeddingEntity, java.util.UUID> {

    @Query(
        value = """
            select * from building_polygon_embeddings bpe
            where bpe.embedding is not null
            order by bpe.embedding <-> :query
            limit :limit
            """,
        nativeQuery = true)
    List<BuildingPolygonEmbeddingEntity> findNearestByEmbedding(float[] query, int limit);

     @Modifying
    @Transactional
    @Query(
        value = """
            update building_polygon_embeddings
            set embedding = :embedding,
                embedding_model = :embeddingModel,
                embedding_source = :embeddingSource,
                embedding_updated_at = :updatedAt
            where polygon_id = :id
            """,
        nativeQuery = true)
    int updateEmbedding(UUID id, float[] embedding, String embeddingModel, String embeddingSource, OffsetDateTime updatedAt);
}
