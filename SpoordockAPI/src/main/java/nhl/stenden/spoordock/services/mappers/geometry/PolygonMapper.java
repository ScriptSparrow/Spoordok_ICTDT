package nhl.stenden.spoordock.services.mappers.geometry;

import java.util.Arrays;
import java.util.List;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonDTO;
import nhl.stenden.spoordock.services.mappers.Mapper;

@Component
public class PolygonMapper implements Mapper<PolygonDTO, Polygon> {

    private final GeometryFactory geometryFactory;

    public PolygonMapper(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    @Override
    public PolygonDTO toDTO(Polygon polygon) {

        var coordinates = Arrays.stream(polygon.getCoordinates())
            .map(coord -> {
                var dto = new Coordinate();
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

    @Override
    public Polygon toEntity(PolygonDTO polygonDTO) {
        var coordinates = polygonDTO.getCoordinates().stream()
            .map(dto -> new org.locationtech.jts.geom.Coordinate(dto.getX(), dto.getY(), dto.getZ()))
            .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        return geometryFactory.createPolygon(coordinates);
    }

    @Override
    public List<PolygonDTO> toDTOs(List<Polygon> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public List<Polygon> toEntities(List<PolygonDTO> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }

}
