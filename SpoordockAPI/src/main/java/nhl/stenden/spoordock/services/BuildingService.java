package nhl.stenden.spoordock.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.database.BuildingTypeRepository;
import nhl.stenden.spoordock.services.mappers.BuildingPolygonMapper;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuildingService {

    private final BuildingPolygonRepository buildingPolygonRepository;
    private final BuildingTypeRepository buildingTypeRepository;
    private final BuildingPolygonMapper buildingPolygonMapper;
    private final BuildingEmbeddingService buildingEmbeddingService;
    

    public BuildingService(BuildingPolygonRepository buildingPolygonRepository, 
                BuildingTypeRepository buildingTypeRepository, 
                BuildingEmbeddingService buildingEmbeddingService,
                BuildingPolygonMapper buildingPolygonMapper
            ) {
        this.buildingPolygonRepository = buildingPolygonRepository;
        this.buildingTypeRepository = buildingTypeRepository;
        this.buildingPolygonMapper = buildingPolygonMapper;
        this.buildingEmbeddingService = buildingEmbeddingService;
    }

    public List<BuildingPolygonDTO> getBuildingPolygons(boolean embedTypes){
        if(embedTypes) {
            var entities = buildingPolygonRepository.findAllIncludingBuildingType();
            return buildingPolygonMapper.toDTOs(entities);
        } 
        else
        {
            var entities = buildingPolygonRepository.findAll();
            return buildingPolygonMapper.toDTOs(entities);
        }
    }

    public boolean buildingTypeExists(BuildingTypeDTO buildingTypeDTO) {

        if(buildingTypeDTO == null) {
            return false;
        }

        return buildingTypeRepository.existsById(buildingTypeDTO.getBuildingTypeId());
    }

    /**
     * Voegt een nieuw gebouw toe aan de database.
     * Gebouwtype is verplicht (database constraint NOT NULL).
     */
    @Transactional
    public BuildingPolygonDTO addBuilding(BuildingPolygonDTO buildingDTO) {
        var entity = buildingPolygonMapper.toEntity(buildingDTO);

        // Verplicht: Gebouwtype ophalen en koppelen (database constraint is NOT NULL)
        if (buildingDTO.getBuildingType() != null && buildingDTO.getBuildingType().getBuildingTypeId() != null) {
            var buildingType = buildingTypeRepository
                .findById(buildingDTO.getBuildingType().getBuildingTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Gebouwtype niet gevonden: " + buildingDTO.getBuildingType().getBuildingTypeId()));
            entity.setBuildingType(buildingType);
        } else {
            throw new IllegalArgumentException("Gebouwtype is verplicht maar ontbreekt in de request");
        }

        var savedEntity = buildingPolygonRepository.save(entity);
        // Geef alleen de ID door aan de achtergrondtaak om race conditions te voorkomen
        buildingEmbeddingService.scheduleEmbeddingTask(savedEntity.getBuildingId());
        return buildingPolygonMapper.toDTO(savedEntity);
    }

    /**
     * Werkt een bestaand gebouw bij in de database.
     * Gebouwtype is verplicht (database constraint NOT NULL).
     * 
     * BELANGRIJK: We halen eerst de BESTAANDE entity op uit de database.
     * Dit triggert de @PostLoad callback die isNew=false zet, waardoor
     * JPA een UPDATE uitvoert in plaats van een INSERT (wat zou falen met
     * "duplicate key" error door de Persistable interface).
     */
    @Transactional
    public BuildingPolygonDTO updateBuilding(BuildingPolygonDTO buildingDTO) {
        // EERST: Haal de bestaande entity op uit de database
        // Dit triggert @PostLoad waardoor isNew = false wordt gezet
        var existingEntity = buildingPolygonRepository.findById(buildingDTO.getBuildingId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Gebouw niet gevonden: " + buildingDTO.getBuildingId()));

        // Update de velden van de BESTAANDE entity (niet een nieuwe aanmaken!)
        existingEntity.setName(buildingDTO.getName());
        existingEntity.setDescription(buildingDTO.getDescription());
        existingEntity.setHeight(buildingDTO.getHeight());
        
        // Polygon coördinaten updaten via de mapper
        var newPolygon = buildingPolygonMapper.toEntity(buildingDTO).getPolygon();
        existingEntity.setPolygon(newPolygon);

        // Verplicht: Gebouwtype ophalen en koppelen (database constraint is NOT NULL)
        if (buildingDTO.getBuildingType() != null && buildingDTO.getBuildingType().getBuildingTypeId() != null) {
            var buildingType = buildingTypeRepository
                .findById(buildingDTO.getBuildingType().getBuildingTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Gebouwtype niet gevonden: " + buildingDTO.getBuildingType().getBuildingTypeId()));
            existingEntity.setBuildingType(buildingType);
        } else {
            throw new IllegalArgumentException("Gebouwtype is verplicht maar ontbreekt in de request");
        }

        // Nu doet save() een UPDATE omdat isNew=false (na @PostLoad)
        var savedEntity = buildingPolygonRepository.save(existingEntity);
        // Geef alleen de ID door aan de achtergrondtaak om race conditions te voorkomen
        buildingEmbeddingService.scheduleEmbeddingTask(savedEntity.getBuildingId());
        return buildingPolygonMapper.toDTO(savedEntity);
    }

    public void deleteBuildingById(java.util.UUID buildingId) {
        buildingPolygonRepository.deleteById(buildingId);
    }

    public Optional<BuildingPolygonDTO> getBuildingById(java.util.UUID buildingId) {
        
        var entityOpt = buildingPolygonRepository.findById(buildingId);
        if(entityOpt.isPresent()) {
            var dto = buildingPolygonMapper.toDTO(entityOpt.get());
            return Optional.of(dto);
        } else {
            return Optional.empty();
        }
    }
}
