package nhl.stenden.spoordock.database.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class RoadSegment {
    // Een weg segment is een deel van een wegen netwerk van een bepaald "weg type"

    @Id
    @Setter(AccessLevel.PRIVATE)
    private UUID id;     // ook een UUID?

//    private String roadName; // ala straatnaam?

    private String roadType; // mogelijk een enum? (Fietspad, Busbaan, Autoweg)

    private String roadDescription; // is deze nodig?

    public RoadSegment(UUID id, String roadType, String roadDescription) {
        this.id = id;
        this.roadType = roadType;
        this.roadDescription = roadDescription;
    }
}