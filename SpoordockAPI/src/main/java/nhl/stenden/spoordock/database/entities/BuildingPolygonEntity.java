package nhl.stenden.spoordock.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

/**
 * Entity voor gebouw-polygonen in de database.
 * 
 * De UUID wordt gegenereerd door de database via gen_random_uuid().
 * 
 * PR1: Persistable<UUID> interface verwijderd - UUID generatie verplaatst
 * van frontend naar database voor betere beveiliging en consistentie.
 */
@Entity
@Table(name = "polygones")
@Getter
@Setter
@NoArgsConstructor
public class BuildingPolygonEntity {

    // PR1: @GeneratedValue toegevoegd - database genereert nu de UUID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID buildingId;

    @Column(name = "naam", nullable = false)
    private String name;

    @Column(name = "omschrijving", length = 450, nullable = false)
    private String description;

    // Verplicht veld: nullable = false komt overeen met database constraint
    @JoinColumn(name = "gebouwtype", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private BuildingTypeEntity buildingType;

    // Verplicht veld: nullable = false komt overeen met database constraint
    @Column(name = "punten", columnDefinition = "geometry(PolygonZ, 4326)", nullable = false)
    private Polygon polygon;

    @Column(name = "hoogte")
    private double height;

    /**
     * Constructor voor het aanmaken van een nieuwe entity zonder ID.
     * De database genereert automatisch een UUID bij het opslaan.
     * 
     * PR1: buildingId parameter verwijderd - database genereert de UUID
     */
    public BuildingPolygonEntity(String name, String description, 
                                  BuildingTypeEntity buildingType, Polygon polygon, double height) {
        this.name = name;
        this.description = description;
        this.buildingType = buildingType;
        this.polygon = polygon;
        this.height = height;
    }
}
