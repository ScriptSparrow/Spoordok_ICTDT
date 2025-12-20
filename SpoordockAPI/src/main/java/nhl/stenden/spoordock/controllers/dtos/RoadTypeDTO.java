package nhl.stenden.spoordock.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RoadTypeDTO {
    //

    private UUID id;
    private int standardWidth;
    private String users;
    private String texture;

    public RoadTypeDTO(UUID id, int standardWidth, String users, String texture) {
        this.id = id;
        this.standardWidth = standardWidth;
        this.users = users;
        this.texture = texture;
    }
}
