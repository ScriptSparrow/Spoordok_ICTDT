package nhl.stenden.spoordock.services;

import java.util.List;

import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;
import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.services.mappers.BuildingPolygonMapper;

@Component
public class BuildingService {
    
    private final BuildingPolygonRepository buildingPolygonRepository;
    private final BuildingPolygonMapper buildingPolygonMapper = new BuildingPolygonMapper();

    public BuildingService(BuildingPolygonRepository buildingPolygonRepository) {
        this.buildingPolygonRepository = buildingPolygonRepository;
    }

    public List<BuildingPolygonDTO> getBuildingPolygons(){
        var entities = buildingPolygonRepository.findAll();
        return buildingPolygonMapper.toDTOs(entities);
    }


}
