package nhl.stenden.spoordock.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RoadSegementDTO {

    private UUID id;
    @NotNull
    private RoadTypeDTO roadType;
    
    private String roadDescription;
    
    // Breedte van het wegsegment in meters
    private int width;
    
    @NotNull
    private List<Coordinate> coordinates;

    public RoadSegementDTO(UUID id, RoadTypeDTO roadType, String roadDescription, int width, List<Coordinate> coordinates) {
        this.id = id;
        this.roadType = roadType;
        this.roadDescription = roadDescription;
        this.width = width;
        this.coordinates = coordinates;
    }
}
