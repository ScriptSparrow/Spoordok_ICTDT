package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

/*
"De RoadTypeRepository verzorgt de standaarddatabasebewerkingen voor wegtypes via Spring Data."
 */

public interface RoadTypeRepository extends ListCrudRepository<RoadTypeTemplate, UUID> {

    // Onderstaande functie bestaat al in CrudRepo
//    Optional<RoadTypeTemplate> findById(UUID id);
}
