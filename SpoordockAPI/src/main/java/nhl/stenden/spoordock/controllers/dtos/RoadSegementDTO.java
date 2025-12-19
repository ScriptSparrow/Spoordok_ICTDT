package nhl.stenden.spoordock.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RoadSegementDTO {

    private UUID id;
    @NotNull
    private String roadType;
    @NotNull
    private String roadDescription;

    public RoadSegementDTO(UUID id, String roadName, String roadDescription) {
        this.id = id;
        this.roadType = roadName;
        this.roadDescription = roadDescription;
    }

}
