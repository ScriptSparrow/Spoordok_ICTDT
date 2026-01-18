package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class RoadTypeMapper implements Mapper<RoadTypeDTO, RoadTypeTemplate>{

    @Override
    public RoadTypeDTO toDTO(RoadTypeTemplate roadType) {
        return new RoadTypeDTO(
                roadType.getId(),
                roadType.getStandardWidth(),
                roadType.getUsers(),
                roadType.getTexture()
        );
    }

    @Override
    public RoadTypeTemplate toEntity(RoadTypeDTO roadTypeDTO) {
        if (roadTypeDTO == null) {
            return null;
        }
        return new RoadTypeTemplate(
                roadTypeDTO.getId(),
                roadTypeDTO.getStandardWidth(),
                roadTypeDTO.getUsers(),
                roadTypeDTO.getTexture()
        );
    }

    // simplified foreach loop, place every in a new list
    @Override
    public List<RoadTypeDTO> toDTOs(List<RoadTypeTemplate> roadTypes) {
        return roadTypes.stream().map(this::toDTO).toList();
    }

    @Override
    public List<RoadTypeTemplate> toEntities(List<RoadTypeDTO> roadTypeDTOS) {
        return roadTypeDTOS.stream().map(this::toEntity).toList();
    }
}
