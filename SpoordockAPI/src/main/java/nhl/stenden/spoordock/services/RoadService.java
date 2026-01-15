package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.database.RoadSegmentRepository;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.services.mappers.RoadSegmentMapper;
import nhl.stenden.spoordock.services.mappers.RoadTypeMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoadService {
    // Verbindende factor tussen database en controller laag qua wegen

    private final RoadTypeRepository roadTypeRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadSegmentMapper roadSegmentMapper;

    public RoadService(RoadTypeRepository roadTypeRepository, RoadSegmentRepository roadSegmentRepository, RoadSegmentMapper roadSegmentMapper)
    {
        this.roadTypeRepository = roadTypeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadSegmentMapper = roadSegmentMapper;
    }

    public List<RoadSegementDTO> getRoadDTOs () {
        var roadSegments = roadSegmentRepository.findAll();
        return roadSegmentMapper.toDTOs(roadSegments);
    }

    public List<RoadTypeDTO> getRoadTypeDTOs() {
        var roadTypes = roadTypeRepository.findAll();
        return new RoadTypeMapper().toDTOs(roadTypes);
    }
    
}
