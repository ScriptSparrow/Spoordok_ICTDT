package nhl.stenden.spoordock.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "polygones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingPolygonEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID buildingId;

    @Column(name = "naam", nullable = false)
    private String name;

    @Column(name = "omschrijving", length = 450)
    private String description;

    @JoinColumn(name = "gebouwtype")
    @ManyToOne(fetch = FetchType.LAZY)
    private BuildingTypeEntity buildingType;

    @Column(name = "punten", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon polygon;

    @Column(name = "hoogte")
    private double height;

}
