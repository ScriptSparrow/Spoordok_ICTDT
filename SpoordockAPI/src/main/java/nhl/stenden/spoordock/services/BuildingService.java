package nhl.stenden.spoordock.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.database.BuildingTypeRepository;
import nhl.stenden.spoordock.services.mappers.BuildingPolygonMapper;

@Component
public class BuildingService {
    
    private final BuildingPolygonRepository buildingPolygonRepository;
    private final BuildingTypeRepository buildingTypeRepository;
    private final BuildingPolygonMapper buildingPolygonMapper;
    private final BuildingEmbeddingService buildingEmbeddingService;
    

    public BuildingService(BuildingPolygonRepository buildingPolygonRepository, 
                BuildingTypeRepository buildingTypeRepository, 
                BuildingEmbeddingService buildingEmbeddingService,
                BuildingPolygonMapper buildingPolygonMapper
            ) {
        this.buildingPolygonRepository = buildingPolygonRepository;
        this.buildingTypeRepository = buildingTypeRepository;
        this.buildingPolygonMapper = buildingPolygonMapper;
        this.buildingEmbeddingService = buildingEmbeddingService;
    }

    public List<BuildingPolygonDTO> getBuildingPolygons(boolean embedTypes){
        if(embedTypes) {
            var entities = buildingPolygonRepository.findAllIncludingBuildingType();
            return buildingPolygonMapper.toDTOs(entities);
        } 
        else
        {
            var entities = buildingPolygonRepository.findAll();
            return buildingPolygonMapper.toDTOs(entities);
        }
    }

    public boolean buildingTypeExists(BuildingTypeDTO buildingTypeDTO) {

        if(buildingTypeDTO == null) {
            return false;
        }

        return buildingTypeRepository.existsById(buildingTypeDTO.getBuildingTypeId());
    }

    public BuildingPolygonDTO addBuilding(BuildingPolygonDTO buildingDTO) {
        var entity = buildingPolygonMapper.toEntity(buildingDTO);
        var savedEntity = buildingPolygonRepository.save(entity);
        buildingEmbeddingService.scheduleEmbeddingTask(savedEntity);
        return buildingPolygonMapper.toDTO(savedEntity);
    }

    
    public BuildingPolygonDTO updateBuilding(BuildingPolygonDTO buildingDTO) {
        var entity = buildingPolygonMapper.toEntity(buildingDTO);
        var savedEntity = buildingPolygonRepository.save(entity);
        buildingEmbeddingService.scheduleEmbeddingTask(savedEntity);
        return buildingPolygonMapper.toDTO(savedEntity);
    }

    public void deleteBuildingById(java.util.UUID buildingId) {
        buildingPolygonRepository.deleteById(buildingId);
    }

    public Optional<BuildingPolygonDTO> getBuildingById(java.util.UUID buildingId) {
        
        var entityOpt = buildingPolygonRepository.findById(buildingId);
        if(entityOpt.isPresent()) {
            var dto = buildingPolygonMapper.toDTO(entityOpt.get());
            return Optional.of(dto);
        } else {
            return Optional.empty();
        }
    }
}
