package nhl.stenden.spoordock.controllers.dtos.polygon;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PolygonDTO {

    List<PolygonCoordinateDTO> coordinates;

}
