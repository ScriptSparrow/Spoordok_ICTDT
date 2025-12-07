package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadDTO;
import nhl.stenden.spoordock.database.entities.RoadSegment;

import java.util.List;
import java.util.stream.Collectors;

public class RoadMapper implements Mapper<RoadDTO, RoadSegment>{

    @Override
    public RoadDTO toDTO(RoadSegment roadSegment) {
        return new RoadDTO(
                roadSegment.getId(),
                roadSegment.getRoadType(),
                roadSegment.getRoadDescription()
        );
    }

    @Override
    public RoadSegment toEntity(RoadDTO roadDTO) {
        return new RoadSegment(
                roadDTO.getId(),
                roadDTO.getRoadType(),
                roadDTO.getRoadDescription()
        );
    }

    // simplified foreach loop, place every in a new list
    @Override
    public List<RoadDTO> toDTOs(List<RoadSegment> roadSegments) {
        return roadSegments.stream().map(this::toDTO).toList();
    }

    @Override
    public List<RoadSegment> toEntities(List<RoadDTO> roadDTOS) {
        return roadDTOS.stream().map(this::toEntity).toList();
    }

    // Moet deze class fungeeren als object, en daarom de Mapper interfeace implementeren
    // of moet de Mapper interface gewoon weg

}
