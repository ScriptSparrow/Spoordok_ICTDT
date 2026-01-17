package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO;
import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.database.entities.RoadTypeTemplate;
import nhl.stenden.spoordock.services.mappers.geometry.LineStringMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoadSegmentMapperTest {

    private RoadTypeMapper roadTypeMapper;
    private LineStringMapper lineStringMapper;
    private RoadSegmentMapper roadSegmentMapper;

    @BeforeEach
    void setUp() {
        roadTypeMapper = mock(RoadTypeMapper.class);
        lineStringMapper = mock(LineStringMapper.class);
        roadSegmentMapper = new RoadSegmentMapper(roadTypeMapper, lineStringMapper);
    }

    @Test
    void toDTO_mapsAllFields_andUsesDependentMappers() {
        // Arrange
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        String description = "Main road segment";

        RoadTypeTemplate roadTypeTemplate = mock(RoadTypeTemplate.class);
        LineString roadPoints = mock(LineString.class);

        RoadSegment entity = mock(RoadSegment.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getRoadTypeTemplate()).thenReturn(roadTypeTemplate);
        when(entity.getRoadDescription()).thenReturn(description);
        when(entity.getRoadPoints()).thenReturn(roadPoints);

        RoadTypeDTO expectedRoadTypeDTO = mock(RoadTypeDTO.class);
        List<Coordinate> expectedCoordinates = List.of(mock(Coordinate.class), mock(Coordinate.class));

        when(roadTypeMapper.toDTO(roadTypeTemplate)).thenReturn(expectedRoadTypeDTO);
        when(lineStringMapper.toDTO(roadPoints)).thenReturn(expectedCoordinates);

        // Act
        RoadSegementDTO dto = roadSegmentMapper.toDTO(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals(expectedRoadTypeDTO, dto.getRoadType());
        assertEquals(description, dto.getRoadDescription());
        assertEquals(expectedCoordinates, dto.getCoordinates());

        verify(roadTypeMapper).toDTO(roadTypeTemplate);
        verify(lineStringMapper).toDTO(roadPoints);
        verifyNoMoreInteractions(roadTypeMapper, lineStringMapper);
    }

    @Test
    void toEntity_mapsAllFields_andUsesDependentMappers() {
        // Arrange
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        String description = "Updated road segment";

        RoadTypeDTO roadTypeDTO = mock(RoadTypeDTO.class);
        List<Coordinate> coordinates = List.of(mock(Coordinate.class));

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);
        when(dto.getRoadType()).thenReturn(roadTypeDTO);
        when(dto.getRoadDescription()).thenReturn(description);
        when(dto.getCoordinates()).thenReturn(coordinates);

        RoadTypeTemplate expectedTemplate = mock(RoadTypeTemplate.class);
        LineString expectedLineString = mock(LineString.class);

        when(roadTypeMapper.toEntity(roadTypeDTO)).thenReturn(expectedTemplate);
        when(lineStringMapper.toEntity(coordinates)).thenReturn(expectedLineString);

        // Act
        RoadSegment entity = roadSegmentMapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(expectedTemplate, entity.getRoadTypeTemplate());
        assertEquals(description, entity.getRoadDescription());
        assertEquals(expectedLineString, entity.getRoadPoints());

        verify(roadTypeMapper).toEntity(roadTypeDTO);
        verify(lineStringMapper).toEntity(coordinates);
        verifyNoMoreInteractions(roadTypeMapper, lineStringMapper);
    }

    @Test
    void toDTOs_mapsList_usingToDTO() {
        // Arrange
        RoadSegment r1 = mock(RoadSegment.class);
        RoadSegment r2 = mock(RoadSegment.class);

        // We spy so we can verify it calls toDTO() internally
        RoadSegmentMapper spyMapper = spy(new RoadSegmentMapper(roadTypeMapper, lineStringMapper));

        RoadSegementDTO d1 = mock(RoadSegementDTO.class);
        RoadSegementDTO d2 = mock(RoadSegementDTO.class);

        doReturn(d1).when(spyMapper).toDTO(r1);
        doReturn(d2).when(spyMapper).toDTO(r2);

        // Act
        List<RoadSegementDTO> result = spyMapper.toDTOs(List.of(r1, r2));

        // Assert
        assertEquals(List.of(d1, d2), result);
        verify(spyMapper).toDTO(r1);
        verify(spyMapper).toDTO(r2);
    }

    @Test
    void toEntities_mapsList_usingToEntity() {
        // Arrange
        RoadSegementDTO d1 = mock(RoadSegementDTO.class);
        RoadSegementDTO d2 = mock(RoadSegementDTO.class);

        RoadSegmentMapper spyMapper = spy(new RoadSegmentMapper(roadTypeMapper, lineStringMapper));

        RoadSegment r1 = mock(RoadSegment.class);
        RoadSegment r2 = mock(RoadSegment.class);

        doReturn(r1).when(spyMapper).toEntity(d1);
        doReturn(r2).when(spyMapper).toEntity(d2);

        // Act
        List<RoadSegment> result = spyMapper.toEntities(List.of(d1, d2));

        // Assert
        assertEquals(List.of(r1, r2), result);
        verify(spyMapper).toEntity(d1);
        verify(spyMapper).toEntity(d2);
    }
}
