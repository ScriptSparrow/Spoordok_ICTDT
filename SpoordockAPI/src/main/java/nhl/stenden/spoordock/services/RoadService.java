package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.database.RoadSegmentRepository;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.services.mappers.RoadSegmentMapper;
import nhl.stenden.spoordock.services.mappers.RoadTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    public List<RoadSegementDTO> getRoadDTOs () {
        var roadSegments = roadSegmentRepository.findAll();
        return new RoadSegmentMapper().toDTOs(roadSegments);
    }


    public List<RoadTypeDTO> getRoadTypeDTOs() {
        var roadTypes = roadTypeRepository.findAll();
        return new RoadTypeMapper().toDTOs(roadTypes);
    }
}
