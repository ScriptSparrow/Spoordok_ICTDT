package nhl.stenden.spoordock.controllers.dtos;

import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import java.util.List;
import java.util.UUID;

/*
"De RoadSegementDTO beschrijft een wegsement zoals de frontend dat gebruikt,
zonder afhankelijk te zijn van database- of geometrie-implementaties."
 */

public class RoadSegmentDTO {
    private UUID id;
    private RoadTypeDTO roadType;
    private String roadDescription;
    private int width;
    private List<Coordinate> coordinates;

    public RoadSegmentDTO() {
    }

    public RoadSegmentDTO(UUID id, RoadTypeDTO roadType, String roadDescription, int width, List<Coordinate> coordinates) {
        this.id = id;
        this.roadType = roadType;
        this.roadDescription = roadDescription;
        this.width = width;
        this.coordinates = coordinates;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoadTypeDTO getRoadType() {
        return roadType;
    }

    public void setRoadType(RoadTypeDTO roadType) {
        this.roadType = roadType;
    }

    public String getRoadDescription() {
        return roadDescription;
    }

    public void setRoadDescription(String roadDescription) {
        this.roadDescription = roadDescription;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }
}
