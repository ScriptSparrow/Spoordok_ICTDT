package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.database.entities.RoadSegment;

import java.util.List;

public class RoadSegmentMapper implements Mapper<RoadSegementDTO, RoadSegment>{

    @Override
    public RoadSegementDTO toDTO(RoadSegment roadSegment) {
        return new RoadSegementDTO(
                roadSegment.getId(),
                roadSegment.getRoadType(),
                roadSegment.getRoadDescription()
        );
    }

    @Override
    public RoadSegment toEntity(RoadSegementDTO roadDTO) {
        return new RoadSegment(
                roadDTO.getId(),
                roadDTO.getRoadType(),
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


}
