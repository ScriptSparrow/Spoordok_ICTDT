package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadSegmentDTO;
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
    private final RoadTypeMapper roadTypeMapper;

    public RoadService(RoadTypeRepository roadTypeRepository, RoadSegmentRepository roadSegmentRepository, RoadSegmentMapper roadSegmentMapper, RoadTypeMapper roadTypeMapper)
    {
        this.roadTypeRepository = roadTypeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadSegmentMapper = roadSegmentMapper;
        this.roadTypeMapper = roadTypeMapper;
    }

    public List<RoadSegmentDTO> getRoadDTOs () {
        var roadSegments = roadSegmentRepository.findAll();
        return roadSegmentMapper.toDTOs(roadSegments);
    }

    public List<RoadTypeDTO> getRoadTypeDTOs() {
        var roadTypes = roadTypeRepository.findAll();
        return roadTypeMapper.toDTOs(roadTypes);
    }

    public void addRoadSegment(RoadSegmentDTO roadSegmentDTO) throws IllegalArgumentException {
        if(roadSegmentRepository.existsById(roadSegmentDTO.getId())){
            throw new IllegalArgumentException("Road segment with ID " + roadSegmentDTO.getId() + " already exists.");
        }

        // Als er geen roadType is meegegeven, gebruik het eerste beschikbare roadtype
        if (roadSegmentDTO.getRoadType() == null || roadSegmentDTO.getRoadType().getId() == null) {
            var defaultRoadType = roadTypeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Geen road types gevonden in de database!"));
            
            roadSegmentDTO = new RoadSegmentDTO(
                roadSegmentDTO.getId(),
                roadTypeMapper.toDTO(defaultRoadType),
                roadSegmentDTO.getRoadDescription(),
                roadSegmentDTO.getWidth(),
                roadSegmentDTO.getCoordinates()
            );
        }

        var entity = roadSegmentMapper.toEntity(roadSegmentDTO);
        roadSegmentRepository.save(entity);
    }

    public void deleteRoadSegment(RoadSegmentDTO roadSegmentDTO)  {
        roadSegmentRepository.deleteById(roadSegmentDTO.getId());
    }

    public void updateRoadSegment(RoadSegmentDTO roadSegmentDTO) throws IllegalArgumentException {
        if(!roadSegmentRepository.existsById(roadSegmentDTO.getId())){
            throw new IllegalArgumentException("Road segment with ID " + roadSegmentDTO.getId() + " does not exist.");
        }

        // Als er geen roadType is meegegeven, gebruik het eerste beschikbare roadtype
        if (roadSegmentDTO.getRoadType() == null || roadSegmentDTO.getRoadType().getId() == null) {
            var defaultRoadType = roadTypeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Geen road types gevonden in de database!"));
            
            roadSegmentDTO = new RoadSegmentDTO(
                roadSegmentDTO.getId(),
                roadTypeMapper.toDTO(defaultRoadType),
                roadSegmentDTO.getRoadDescription(),
                roadSegmentDTO.getWidth(),
                roadSegmentDTO.getCoordinates()
            );
        }

        var entity = roadSegmentMapper.toEntity(roadSegmentDTO);
        roadSegmentRepository.save(entity);
    }

}