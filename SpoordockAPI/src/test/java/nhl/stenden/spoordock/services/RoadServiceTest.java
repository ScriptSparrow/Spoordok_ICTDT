package nhl.stenden.spoordock.services;

import nhl.stenden.spoordock.controllers.dtos.RoadSegementDTO;
import nhl.stenden.spoordock.database.RoadSegmentRepository;
import nhl.stenden.spoordock.database.RoadTypeRepository;
import nhl.stenden.spoordock.database.entities.RoadSegment;
import nhl.stenden.spoordock.services.mappers.RoadSegmentMapper;
import nhl.stenden.spoordock.services.mappers.RoadTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadServiceTest {

    @Mock
    private RoadTypeRepository roadTypeRepository;

    @Mock
    private RoadSegmentRepository roadSegmentRepository;

    @Mock
    private RoadSegmentMapper roadSegmentMapper;

    @Mock
    private RoadTypeMapper roadTypeMapper;

    @InjectMocks
    private RoadService roadService;

    @Test
    void getRoadDTOs_returnsMappedDTOs() {
        // Arrange
        List<RoadSegment> entities = List.of(mock(RoadSegment.class), mock(RoadSegment.class));
        List<RoadSegementDTO> dtos = List.of(mock(RoadSegementDTO.class), mock(RoadSegementDTO.class));

        when(roadSegmentRepository.findAll()).thenReturn(entities);
        when(roadSegmentMapper.toDTOs(entities)).thenReturn(dtos);

        // Act
        List<RoadSegementDTO> result = roadService.getRoadDTOs();

        // Assert
        assertSame(dtos, result);
        verify(roadSegmentRepository).findAll();
        verify(roadSegmentMapper).toDTOs(entities);
        verifyNoMoreInteractions(roadSegmentRepository, roadSegmentMapper);
        verifyNoInteractions(roadTypeRepository, roadTypeMapper);
    }

    @Test
    void getRoadTypeDTOs_returnsMappedDTOs() {
        // Arrange
        var roadTypeEntities = List.of(
                mock(nhl.stenden.spoordock.database.entities.RoadTypeTemplate.class),
                mock(nhl.stenden.spoordock.database.entities.RoadTypeTemplate.class)
        );

        var roadTypeDTOs = List.of(
                mock(nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO.class),
                mock(nhl.stenden.spoordock.controllers.dtos.RoadTypeDTO.class)
        );

        when(roadTypeRepository.findAll()).thenReturn(roadTypeEntities);
        when(roadTypeMapper.toDTOs(roadTypeEntities)).thenReturn(roadTypeDTOs);

        // Act
        var result = roadService.getRoadTypeDTOs();

        // Assert
        assertSame(roadTypeDTOs, result);

        verify(roadTypeRepository).findAll();
        verify(roadTypeMapper).toDTOs(roadTypeEntities);
        verifyNoMoreInteractions(roadTypeRepository, roadTypeMapper);

        verifyNoInteractions(roadSegmentRepository, roadSegmentMapper);
    }

    @Test
    void addRoadSegment_throwsException_whenIdAlreadyExists() {
        // Arrange
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);
        when(roadSegmentRepository.existsById(id)).thenReturn(true);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roadService.addRoadSegment(dto)
        );

        assertTrue(ex.getMessage().contains(id.toString()));

        verify(roadSegmentRepository).existsById(id);
        verifyNoMoreInteractions(roadSegmentRepository);
        verifyNoInteractions(roadSegmentMapper, roadTypeRepository, roadTypeMapper);
    }

    @Test
    void addRoadSegment_savesEntity_whenIdDoesNotExist() {
        // Arrange
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);

        when(roadSegmentRepository.existsById(id)).thenReturn(false);

        RoadSegment entity = mock(RoadSegment.class);
        when(roadSegmentMapper.toEntity(dto)).thenReturn(entity);

        // Act
        roadService.addRoadSegment(dto);

        // Assert
        verify(roadSegmentRepository).existsById(id);
        verify(roadSegmentMapper).toEntity(dto);
        verify(roadSegmentRepository).save(entity);
        verifyNoMoreInteractions(roadSegmentRepository, roadSegmentMapper);
        verifyNoInteractions(roadTypeRepository, roadTypeMapper);
    }

    @Test
    void deleteRoadSegment_deletesById() {
        // Arrange
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);

        // Act
        roadService.deleteRoadSegment(dto);

        // Assert
        verify(roadSegmentRepository).deleteById(id);
        verifyNoMoreInteractions(roadSegmentRepository);
        verifyNoInteractions(roadSegmentMapper, roadTypeRepository, roadTypeMapper);
    }

    @Test
    void updateRoadSegment_throwsException_whenIdDoesNotExist() {
        // Arrange
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);

        when(roadSegmentRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roadService.updateRoadSegment(dto)
        );

        assertTrue(ex.getMessage().contains(id.toString()));

        verify(roadSegmentRepository).existsById(id);
        verifyNoMoreInteractions(roadSegmentRepository);
        verifyNoInteractions(roadSegmentMapper, roadTypeRepository, roadTypeMapper);
    }

    @Test
    void updateRoadSegment_savesEntity_whenIdExists() {
        // Arrange
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");

        RoadSegementDTO dto = mock(RoadSegementDTO.class);
        when(dto.getId()).thenReturn(id);

        when(roadSegmentRepository.existsById(id)).thenReturn(true);

        RoadSegment entity = mock(RoadSegment.class);
        when(roadSegmentMapper.toEntity(dto)).thenReturn(entity);

        // Act
        roadService.updateRoadSegment(dto);

        // Assert
        verify(roadSegmentRepository).existsById(id);
        verify(roadSegmentMapper).toEntity(dto);

        ArgumentCaptor<RoadSegment> captor = ArgumentCaptor.forClass(RoadSegment.class);
        verify(roadSegmentRepository).save(captor.capture());
        assertSame(entity, captor.getValue());

        verifyNoMoreInteractions(roadSegmentRepository, roadSegmentMapper);
        verifyNoInteractions(roadTypeRepository, roadTypeMapper);
    }
}
