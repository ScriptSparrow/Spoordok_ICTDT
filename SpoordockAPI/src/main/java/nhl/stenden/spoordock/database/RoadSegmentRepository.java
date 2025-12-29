package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadSegment;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface RoadSegmentRepository extends ListCrudRepository<RoadSegment, UUID> {

    // ListCrudRepository ipv CrudRepo vanwege List functionaliteit

}
