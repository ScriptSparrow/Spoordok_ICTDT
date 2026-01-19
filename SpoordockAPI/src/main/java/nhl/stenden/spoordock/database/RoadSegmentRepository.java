package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadSegment;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

/*
"De RoadSegementRepository verzorgt de basisdatabasebewerkingen voor roadsegmenten via Spring Data."
 */

public interface RoadSegmentRepository extends ListCrudRepository<RoadSegment, UUID> {

    // ListCrudRepository ipv CrudRepo vanwege List functionaliteit

}
