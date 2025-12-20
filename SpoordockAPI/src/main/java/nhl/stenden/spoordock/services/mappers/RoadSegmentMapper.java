package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import org.locationtech.jts.geom.LineString;

import java.util.List;

public class RoadSegmentMapper implements Mapper<RoadSegementDTO, RoadSegment>{

    private final RoadTypeMapper roadTypeMapper = new RoadTypeMapper();

       @Override
    public RoadSegementDTO toDTO(RoadSegment roadSegment) {

        RoadTypeDTO roadTypeDTO = roadTypeMapper.toDTO(roadSegment.getRoadTypeTemplate());

        return new RoadSegementDTO(
                roadSegment.getId(),
                roadTypeDTO,
                roadSegment.getRoadDescription()
        );
    }

    @Override
    public RoadSegment toEntity(RoadSegementDTO roadDTO) {

           RoadTypeTemplate roadTypeTemplate = roadTypeMapper.toEntity(roadDTO.getRoadType());

           return new RoadSegment(
                roadDTO.getId(),
                roadTypeTemplate,
                roadDTO.getRoadDescription()
        );
    }

    // simplified foreach loop, place every in a new list
    @Override
    public List<RoadSegementDTO> toDTOs(List<RoadSegment> roadSegments) {
        return roadSegments.stream().map(this::toDTO).toList();
    }

    @Override
    public List<RoadSegment> toEntities(List<RoadSegementDTO> roadDTOS) {
        return roadDTOS.stream().map(this::toEntity).toList();
    }

    private List<Coordinate> mapLineString(LineString lineString) {

    }

    private LineString mapCoordinates(List<Coordinate> coordinates) {

    }

}
