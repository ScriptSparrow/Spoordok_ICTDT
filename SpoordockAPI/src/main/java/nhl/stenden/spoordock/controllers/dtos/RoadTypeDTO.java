package nhl.stenden.spoordock.controllers.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/*
"De RoadTypeDTO beschrijft een wegtype zoals het via de API beschikbaar wordt gesteld."
 */

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
