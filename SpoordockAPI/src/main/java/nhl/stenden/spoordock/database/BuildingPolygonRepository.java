package nhl.stenden.spoordock.database;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;

public interface BuildingPolygonRepository extends ListCrudRepository<BuildingPolygonEntity, UUID> {

    @EntityGraph(attributePaths = "buildingType")
    @Query("select bp from BuildingPolygonEntity bp")
    List<BuildingPolygonEntity> findAllIncludingBuildingType();

    /**
     * Haalt een gebouw op inclusief het gebouwtype (eager fetch).
     * Nodig voor achtergrondtaken die buiten de oorspronkelijke transactie draaien.
     */
    @EntityGraph(attributePaths = "buildingType")
    @Query("select bp from BuildingPolygonEntity bp where bp.buildingId = :id")
    Optional<BuildingPolygonEntity> findByIdIncludingBuildingType(@Param("id") UUID id);

}
