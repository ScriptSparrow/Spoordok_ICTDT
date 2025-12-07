package nhl.stenden.spoordock.services.mappers;

import java.util.Arrays;
import java.util.List;

import org.locationtech.jts.geom.Polygon;
import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonDTO;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonCoordinateDTO;
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
                setPolygon(mapPolygon(entity.getPolygon()));

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
                setPolygon(mapPolygon(dto.getPolygon()));

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

    private PolygonDTO mapPolygon(Polygon polygon) {

        var coordinates = Arrays.stream(polygon.getCoordinates())
            .map(coord -> {
                var dto = new PolygonCoordinateDTO();
                dto.setX(coord.getX());
                dto.setY(coord.getY());
                dto.setZ(coord.getZ());
                return dto;
            })
            .toList();

        var polygonDTO = new PolygonDTO();
        polygonDTO.setCoordinates(coordinates);
        return polygonDTO;
    }

    private Polygon mapPolygon(PolygonDTO polygonDTO) {
        
        var coordinates = polygonDTO.getCoordinates().stream()
            .map(dto -> new org.locationtech.jts.geom.Coordinate(dto.getX(), dto.getY(), dto.getZ()))
            .toArray(org.locationtech.jts.geom.Coordinate[]::new);

        var geometryFactory = new org.locationtech.jts.geom.GeometryFactory();
        return geometryFactory.createPolygon(coordinates);
    }
}
