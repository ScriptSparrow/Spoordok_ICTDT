package nhl.stenden.spoordock.services.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.database.entities.BuildingTypeEntity;

@Component
public class BuildingTypeMapper implements Mapper<BuildingTypeDTO, BuildingTypeEntity> {

    @Override
    public BuildingTypeDTO toDTO(BuildingTypeEntity entity) {
        
        return new BuildingTypeDTO(
            entity.getTypeId(),
            entity.getName(),
            entity.getDescription(),
            entity.getUnit(),
            entity.getCostPerUnit(),
            entity.isInhabitable(),
            entity.getResidentsPerUnit(),
            entity.getPoints(),
            entity.getColor()
        );
    }

    @Override
    public BuildingTypeEntity toEntity(BuildingTypeDTO dto) {
        return new BuildingTypeEntity(
            dto.getBuildingTypeId(),
            dto.getLabelName(),
            dto.getDescription(),
            dto.getUnit(),
            dto.getCostPerUnit(),
            dto.isInhabitable(),
            dto.getResidentsPerUnit(),
            dto.getPoints(),
            dto.getColor()
        );
    }

    @Override
    public List<BuildingTypeDTO> toDTOs(List<BuildingTypeEntity> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public List<BuildingTypeEntity> toEntities(List<BuildingTypeDTO> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }

}
