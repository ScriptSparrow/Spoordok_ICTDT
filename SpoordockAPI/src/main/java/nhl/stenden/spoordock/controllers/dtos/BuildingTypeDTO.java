package nhl.stenden.spoordock.controllers.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingTypeDTO {

    // Identifier
    private UUID buildingTypeId;

    // LabelName in Human readable format
    private String labelName;

    // Description of the building type
    private String description;

    // unit (m3, m2 etc)
    private String unit;

    //Cost per unit
    private String costPerUnit;

    // Is the building inhabitable
    private boolean inhabitable;

    // number of residents per unit
    private Double residentsPerUnit;

    // points awarded for building this type per unit
    private int points;    

    // Color representation in the frontend in hex format
    private String color;

}
