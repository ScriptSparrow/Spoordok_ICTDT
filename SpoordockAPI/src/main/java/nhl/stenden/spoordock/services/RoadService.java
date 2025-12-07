package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadDTO;
import nhl.stenden.spoordock.database.RoadSegmentRepository;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.services.mappers.Mapper;
import nhl.stenden.spoordock.services.mappers.RoadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoadService {
    // Verbindende factor tussen database en controller laag qua wegen

    private final RoadTypeRepository roadTypeRepository;
    private final RoadSegmentRepository roadSegmentRepository;

    @Autowired
    public RoadService(RoadTypeRepository roadTypeRepository, RoadSegmentRepository roadSegmentRepository)
    {
        this.roadTypeRepository = roadTypeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
    }

    public List<RoadDTO> getRoadDTOs () {
        var segments = roadSegmentRepository.findAll();
        return new RoadMapper().toDTOs(segments);
    }


}
