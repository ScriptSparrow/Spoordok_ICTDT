package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoadTypeRepository extends CrudRepository<RoadTypeTemplate, UUID> {

    // Onderstaande functie bestaat al in CrudRepo
//    Optional<RoadTypeTemplate> findById(UUID id);
}
