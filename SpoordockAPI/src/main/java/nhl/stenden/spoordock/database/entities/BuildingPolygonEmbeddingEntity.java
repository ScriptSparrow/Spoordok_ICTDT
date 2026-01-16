package nhl.stenden.spoordock.database.entities;

import java.time.OffsetDateTime;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "polygon_embeddings")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BuildingPolygonEmbeddingEntity {

    @Id
    @Column(name = "polygon_id", updatable = false, nullable = false)
    private java.util.UUID buildingId;

    //LLM Specific fields
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_source")
    private String embeddingSource;

    @Column(name = "embedding_updated_at")
    private OffsetDateTime embeddingUpdatedAt;

}
