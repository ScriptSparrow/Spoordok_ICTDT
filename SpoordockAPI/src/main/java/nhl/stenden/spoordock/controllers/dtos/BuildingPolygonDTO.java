package nhl.stenden.spoordock.controllers.dtos;

import java.util.UUID;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BuildingPolygonDTO {

    private UUID buildingId;
    private String name;
    
    @Nullable
    private BuildingTypeDTO buildingType;

}
