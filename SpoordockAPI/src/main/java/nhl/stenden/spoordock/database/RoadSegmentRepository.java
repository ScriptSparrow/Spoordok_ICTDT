package nhl.stenden.spoordock.database;

import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoadSegmentRepository extends ListCrudRepository<RoadSegment, UUID> {

    // ListCrudRepository ipv CrudRepo vanwege List functionaliteit

}
