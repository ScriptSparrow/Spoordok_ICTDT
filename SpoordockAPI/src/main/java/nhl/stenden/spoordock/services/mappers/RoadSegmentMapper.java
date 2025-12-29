package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import nhl.stenden.spoordock.services.mappers.geometry.LineStringMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoadSegmentMapper implements Mapper<RoadSegementDTO, RoadSegment> {

    private final RoadTypeMapper roadTypeMapper;
    private final LineStringMapper lineStringMapper;

    public RoadSegmentMapper(RoadTypeMapper roadTypeMapper, LineStringMapper lineStringMapper) {
        this.roadTypeMapper = roadTypeMapper;
        this.lineStringMapper = lineStringMapper;
    }

    @Override
    public RoadSegementDTO toDTO(RoadSegment roadSegment) {

        RoadTypeDTO roadTypeDTO = roadTypeMapper.toDTO(roadSegment.getRoadTypeTemplate());
        List<Coordinate> coordinates = lineStringMapper.toDTO(roadSegment.getRoadPoints());
        return new RoadSegementDTO(
                roadSegment.getId(),
                roadTypeDTO,
                roadSegment.getRoadDescription(),
                coordinates);
    }

    @Override
    public RoadSegment toEntity(RoadSegementDTO roadDTO) {

        RoadTypeTemplate roadTypeTemplate = roadTypeMapper.toEntity(roadDTO.getRoadType());
        org.locationtech.jts.geom.LineString roadPoints = lineStringMapper.toEntity(roadDTO.getCoordinates());
        return new RoadSegment(
                roadDTO.getId(),
                roadTypeTemplate,
                roadDTO.getRoadDescription(),
                roadPoints);
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
}
