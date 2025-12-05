package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadDTO;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.services.mappers.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoadService implements Mapper<RoadDTO, RoadSegment> {
    // Verbindende factor tussen database en controller laag qua wegen

    private final RoadTypeRepository roadTypeRepository;

    @Autowired
    public RoadService(RoadTypeRepository roadTypeRepository)
    {
        this.roadTypeRepository = roadTypeRepository;
    }

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
        return null;
    }

    @Override
    public List<RoadDTO> toDTOs(List<RoadSegment> roadSegments) {
        return List.of();
    }
}
