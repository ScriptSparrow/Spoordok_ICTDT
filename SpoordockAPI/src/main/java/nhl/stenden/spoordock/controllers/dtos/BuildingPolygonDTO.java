package nhl.stenden.spoordock.controllers.dtos;

import java.util.UUID;

import org.springframework.lang.NonNull;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonDTO;

@Getter
@Setter
@NoArgsConstructor
public class BuildingPolygonDTO {

    private UUID buildingId;
    
    @NonNull
    private String name;
    
    @NonNull
    private String description;
    
    @Nullable
    private BuildingTypeDTO buildingType;

    
    
    @NonNull
    private PolygonDTO polygon;

}
