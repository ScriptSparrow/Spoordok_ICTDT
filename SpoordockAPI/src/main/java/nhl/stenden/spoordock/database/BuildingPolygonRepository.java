package nhl.stenden.spoordock.database;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;

public interface BuildingPolygonRepository extends ListCrudRepository<BuildingPolygonEntity, UUID> {

    @EntityGraph(attributePaths = "buildingType")
    @Query("select bp from BuildingPolygonEntity bp")
    List<BuildingPolygonEntity> findAllIncludingBuildingType();

  

}
