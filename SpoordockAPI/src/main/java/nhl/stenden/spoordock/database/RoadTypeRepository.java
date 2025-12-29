package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoadTypeRepository extends ListCrudRepository<RoadTypeTemplate, UUID> {

    // Onderstaande functie bestaat al in CrudRepo
//    Optional<RoadTypeTemplate> findById(UUID id);
}
