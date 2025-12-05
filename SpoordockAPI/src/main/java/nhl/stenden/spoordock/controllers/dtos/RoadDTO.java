package nhl.stenden.spoordock.controllers.dtos;

import java.util.UUID;

public class RoadDTO {

    private UUID id;
    private String roadType;
    private String roadDescription;

    public RoadDTO(UUID id, String roadName, String roadDescription) {
        this.id = id;
        this.roadType = roadName;
        this.roadDescription = roadDescription;
    }

    public RoadDTO(UUID id, String roadType) {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRoadType() {
        return roadType;
    }

    public void setRoadType(String roadType) {
        this.roadType = roadType;
    }

    public String getRoadDescription() {
        return roadDescription;
    }

    public void setRoadDescription(String roadDescription) {
        this.roadDescription = roadDescription;
    }
}
