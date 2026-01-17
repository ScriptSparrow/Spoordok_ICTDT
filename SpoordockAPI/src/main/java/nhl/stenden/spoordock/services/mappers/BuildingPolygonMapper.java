package nhl.stenden.spoordock.services.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.services.mappers.geometry.PolygonMapper;

@Component
public class BuildingPolygonMapper implements Mapper<BuildingPolygonDTO, BuildingPolygonEntity> {

    private final BuildingTypeMapper buildingTypeMapper;
    private final PolygonMapper polygonMapper;

    public BuildingPolygonMapper(BuildingTypeMapper buildingTypeMapper, PolygonMapper polygonMapper) {
        this.buildingTypeMapper = buildingTypeMapper;
        this.polygonMapper = polygonMapper;
    }

    @Override
    public BuildingPolygonDTO toDTO(BuildingPolygonEntity entity) {
        return new BuildingPolygonDTO(
            entity.getBuildingId(),
            entity.getName(),
            entity.getDescription(),
            entity.getBuildingType() != null ? buildingTypeMapper.toDTO(entity.getBuildingType()) : null,
            polygonMapper.toDTO(entity.getPolygon()),
            entity.getHeight()
        );
    }

    /**
     * Converteert een DTO naar een nieuwe entity.
     * 
     * PR1: buildingId wordt niet meer meegegeven - de database genereert
     * automatisch een UUID via gen_random_uuid().
     */
    @Override
    public BuildingPolygonEntity toEntity(BuildingPolygonDTO dto) {

        return new BuildingPolygonEntity(
            dto.getName(), 
            dto.getDescription(),
            null, // Koppeling met gebouwtype wordt in de BuildingService gedaan om validatiefouten te voorkomen
            polygonMapper.toEntity(dto.getPolygon()),
            dto.getHeight()
        );
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
