package nhl.stenden.spoordock.database;

import java.util.List;

import org.locationtech.jts.geom.Polygon;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
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

    @Query(
        value =  """
            select bpe.* from building_polygon_embeddings bpe
            join polygones p on bpe.building_id = p.id
            where ST_Within(p.punten, :zone)
              and bpe.embedding is not null
            order by bpe.embedding <-> :query
            limit :limit
        """,
        nativeQuery = true
    )
    List<BuildingPolygonEmbeddingEntity> promptBuildingsInZone(float[] query, Polygon zone, int limit);

}
