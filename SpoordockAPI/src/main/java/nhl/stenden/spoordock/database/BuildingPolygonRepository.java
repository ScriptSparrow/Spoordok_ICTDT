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

    /**
     * Haalt alle gebouw-IDs op met hun oppervlakte in vierkante meters berekend door PostGIS.
     * Gebruikt ST_Area met geography cast voor nauwkeurige berekening op geodetische coördinaten.
     * 
     * @return Lijst van Object[] arrays waarbij [0] = UUID (building ID) en [1] = Double (area in m²)
     */
    @Query(value = "SELECT id, ST_Area(punten::geography) as area_m2 FROM polygones", nativeQuery = true)
    List<Object[]> findAllBuildingAreasInSquareMeters();

}
