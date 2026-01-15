package nhl.stenden.spoordock.controllers.dtos;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingPolygonDTO {

    @Nullable
    private UUID buildingId;
    
    @NotNull(message = "Name cannot be null")
    private String name;
    
    @NotNull(message = "Description cannot be null")
    private String description;
    
//    @Nullable
//    private BuildingTypeDTO buildingType;

    @Nullable
    private BuildingTypeDTO2 buildingType;

    @NotNull(message = "Polygon cannot be null")
    private PolygonDTO polygon;
    
    @NotNull(message = "Height cannot be null")
    
    @Min(value = 0, message = "Height must be non-negative")
    private double height;
}
