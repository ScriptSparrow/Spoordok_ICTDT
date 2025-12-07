package nhl.stenden.spoordock.database.entities;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class RoadTypeTemplate {
    // RoadTypeTemplate is het template voor de te maken RoadSegments

    @Id
    @Setter(AccessLevel.PRIVATE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @OneToMany(mappedBy = "roadSegment")
    private List<RoadTypeTemplate> roadTemplates;

    @Column(name = "standaard_breedte")
    private int standardWidth;

    @Column(name = "gebruikers")
    private String users;

    private String texture;

}
