package nhl.stenden.spoordock.database.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

import java.util.UUID;

@Entity
@Table(name = "wegsegmenten")
@Getter
@Setter
public class RoadSegment {
    // Een weg segment is een deel van een wegen netwerk van een bepaald "weg type"

    @Id
    @Setter(AccessLevel.PRIVATE)
    private UUID id;     // ook een UUID?

    private String roadDescription; // is deze nodig?

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wegtype")
    private RoadTypeTemplate roadTypeTemplate;

    @Column(name = "points")
    private LineString roadPoints;

    public RoadSegment() {}

    public RoadSegment(UUID id, RoadTypeTemplate roadTypeTemplate, String roadDescription, LineString roadPoints) {
        this.id = id;
        this.roadTypeTemplate = roadTypeTemplate;
        this.roadDescription = roadDescription;
        this.roadPoints = roadPoints;
    }
}