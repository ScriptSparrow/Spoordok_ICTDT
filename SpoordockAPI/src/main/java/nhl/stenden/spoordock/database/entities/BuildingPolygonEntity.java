package nhl.stenden.spoordock.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.domain.Persistable;

/**
 * Entity voor gebouw-polygonen in de database.
 * 
 * Implementeert Persistable<UUID> om Spring Data JPA expliciet te vertellen
 * wanneer een entity "nieuw" is. Dit is nodig omdat de frontend zelf UUID's
 * genereert, waardoor JPA anders een merge() zou proberen in plaats van persist().
 * 
 * Zonder deze interface zou een StaleObjectStateException optreden bij het opslaan
 * van nieuwe polygonen met een door de frontend gegenereerde UUID.
 */
@Entity
@Table(name = "polygones")
@Getter
@Setter
@NoArgsConstructor
public class BuildingPolygonEntity implements Persistable<UUID> {

    @Id
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
     * Transient veld om bij te houden of de entity nieuw is.
     * Dit wordt niet opgeslagen in de database.
     * Standaard true voor nieuwe entities (aangemaakt via constructor).
     */
    @Transient
    private boolean isNew = true;

    /**
     * Constructor voor het aanmaken van een nieuwe entity.
     * isNew blijft true zodat JPA weet dat dit een INSERT moet worden.
     */
    public BuildingPolygonEntity(UUID buildingId, String name, String description, 
                                  BuildingTypeEntity buildingType, Polygon polygon, double height) {
        this.buildingId = buildingId;
        this.name = name;
        this.description = description;
        this.buildingType = buildingType;
        this.polygon = polygon;
        this.height = height;
        this.isNew = true; // Expliciet markeren als nieuw
    }

    // === Persistable interface implementatie ===

    /**
     * Retourneert de ID van deze entity (vereist door Persistable).
     */
    @Override
    public UUID getId() {
        return buildingId;
    }

    /**
     * Vertelt Spring Data JPA of deze entity nieuw is.
     * Als true: JPA voert een INSERT (persist) uit.
     * Als false: JPA voert een UPDATE (merge) uit.
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Callback na het laden uit de database.
     * Markeert de entity als "niet nieuw" omdat het al in de database staat.
     */
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    /**
     * Callback na het opslaan in de database.
     * Markeert de entity als "niet nieuw" zodat volgende saves een UPDATE worden.
     */
    @PostPersist
    void markNotNewAfterPersist() {
        this.isNew = false;
    }
}
