package nhl.stenden.spoordock.services.mappers;

import java.util.List;

import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;

import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;

public class BuildingPolygonMapper implements Mapper<BuildingPolygonDTO, BuildingPolygonEntity> {

    private final BuildingTypeMapper buildingTypeMapper;

    public BuildingPolygonMapper() {
        this.buildingTypeMapper = new BuildingTypeMapper();
    }

    @Override
    public BuildingPolygonDTO toDTO(BuildingPolygonEntity entity) {
       
        return new BuildingPolygonDTO()
        {
            {
                setBuildingId(entity.getBuildingId());
                setName(entity.getName());

                if (entity.getBuildingType() != null) {
                    setBuildingType(buildingTypeMapper.toDTO(entity.getBuildingType()));
                } else {
                    setBuildingType(null);
                }
            }
        };

    }

    @Override
    public BuildingPolygonEntity toEntity(BuildingPolygonDTO dto) {
       return new BuildingPolygonEntity()
       {
        {
            setBuildingId(dto.getBuildingId());
            setName(dto.getName());

            if (dto.getBuildingType() != null) {
                setBuildingType(buildingTypeMapper.toEntity(dto.getBuildingType()));
            } else {
                setBuildingType(null);
            }
        }
       };
        
    }

    @Override
    public List<BuildingPolygonDTO> toDTOs(List<BuildingPolygonEntity> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public List<BuildingPolygonEntity> toEntities(List<BuildingPolygonDTO> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }
    
    




}
