package nhl.stenden.spoordock.database;

import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;

public interface BuildingPolygonRepository extends ListCrudRepository<BuildingPolygonEntity, UUID> {

}
