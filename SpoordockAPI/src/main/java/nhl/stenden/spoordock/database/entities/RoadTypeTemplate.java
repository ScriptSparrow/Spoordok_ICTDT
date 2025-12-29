package nhl.stenden.spoordock.database.entities;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "wegtypes")
@Getter
@Setter
public class RoadTypeTemplate {
    // RoadTypeTemplate is het template voor de te maken RoadSegments

    @Id
    @Setter(AccessLevel.PRIVATE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

<<<<<<< HEAD
    // @OneToMany(mappedBy = "roadSegment")
    // private List<RoadTypeTemplate> roadTemplates;

=======
>>>>>>> RvO/Roads_feature
    @Column(name = "standaard_breedte")
    private int standardWidth;

    @Column(name = "gebruikers")
    private String users;

    private String texture;

    public RoadTypeTemplate() {}

    public RoadTypeTemplate(UUID id, int standardWidth, String users, String texture) {
        this.id = id;
        this.standardWidth = standardWidth;
        this.users = users;
        this.texture = texture;
    }

}