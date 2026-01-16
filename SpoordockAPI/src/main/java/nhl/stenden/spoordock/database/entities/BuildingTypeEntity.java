package nhl.stenden.spoordock.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gebouwtypes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingTypeEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID typeId;

    @Column(name = "naam", nullable = false)
    private String name;

    @Column(name = "omschrijving")
    private String description;

    @Column(name = "eenheid")
    private String unit;

    @Column(name = "kosten_per_eenheid")
    private double costPerUnit;

    @Column(name = "bewoonbaar")
    private boolean inhabitable;

    @Column(name = "bewoners_per_eenheid")
    private Double residentsPerUnit;

    @Column(name = "punten_per_eenheid")
    private Integer points;

    @Column(name = "kleur")
    private String color;

}
