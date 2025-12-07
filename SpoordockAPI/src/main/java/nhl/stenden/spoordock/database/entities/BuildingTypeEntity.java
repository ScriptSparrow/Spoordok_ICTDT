package nhl.stenden.spoordock.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "gebouwtypes")
@Getter
@Setter
public class BuildingTypeEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID typeId;

    @Column(name = "naam", nullable = false)
    private String name;

    @Column(name = "omschrijving")
    private String description;

    @Column(name = "verdiepings_hoogte")
    private double floorHeight;

    @Column(name = "kleur")
    private String color;

}
