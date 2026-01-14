package nhl.stenden.spoordock.services;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.database.BuildingTypeRepository;
import nhl.stenden.spoordock.services.mappers.BuildingTypeMapper;

@Component
public class BuildingTypeService {

    private final BuildingTypeMapper buildingTypeMapper;
    private final BuildingTypeRepository buildingTypeRepository;

    public BuildingTypeService(
        BuildingTypeMapper buildingTypeMapper,
        BuildingTypeRepository buildingTypeRepository
    ){
        this.buildingTypeMapper = buildingTypeMapper;
        this.buildingTypeRepository = buildingTypeRepository;
    }

    public List<BuildingTypeDTO> getBuildingTypes() {
        var entities = buildingTypeRepository.findAll();
        return buildingTypeMapper.toDTOs(entities);
    }
    
}
