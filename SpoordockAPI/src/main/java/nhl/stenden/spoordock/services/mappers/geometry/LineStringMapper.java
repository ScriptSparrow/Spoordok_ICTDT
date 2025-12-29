package nhl.stenden.spoordock.services.mappers.geometry;

import java.util.Arrays;
import java.util.List;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;
import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.services.mappers.Mapper;

@Component
public class LineStringMapper implements Mapper<List<Coordinate>, LineString> {

    private final GeometryFactory geometryFactory;

    public LineStringMapper(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }
    
    @Override
    public List<Coordinate> toDTO(LineString lineString) {
        return Arrays.stream(lineString.getCoordinates())
                .map(
                    x-> {
                        Coordinate coord = new Coordinate();
                        coord.setX(x.getX());
                        coord.setY(x.getY());
                        coord.setZ(x.getZ());
                        return coord;
                    }
                ).toList();
    }

    @Override
    public LineString toEntity(List<Coordinate> coordinates) {
       
        org.locationtech.jts.geom.Coordinate[] coords = coordinates.stream()
            .map(dto -> new org.locationtech.jts.geom.Coordinate(dto.getX(), dto.getY(), dto.getZ()))
            .toArray(org.locationtech.jts.geom.Coordinate[]::new);

        return geometryFactory.createLineString(coords);

    }

    @Override
    public List<List<Coordinate>> toDTOs(List<LineString> entities) {
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public List<LineString> toEntities(List<List<Coordinate>> dtos) {
        return dtos.stream().map(this::toEntity).toList();
    }
}
