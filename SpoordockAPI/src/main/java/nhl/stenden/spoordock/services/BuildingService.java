package nhl.stenden.spoordock.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import nhl.stenden.spoordock.backgroundprocessor.BackgroundProcessor;
import nhl.stenden.spoordock.controllers.dtos.BuildingPolygonDTO;
import nhl.stenden.spoordock.controllers.dtos.BuildingTypeDTO;
import nhl.stenden.spoordock.database.BuildingPolygonEmbeddingRepository;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.database.BuildingTypeRepository;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEmbeddingEntity;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.llmService.OllamaConnectorService;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolFunctionCall;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolParameter;
import nhl.stenden.spoordock.services.mappers.BuildingEmbeddingMapper;
import nhl.stenden.spoordock.services.mappers.BuildingPolygonMapper;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuildingService {

    private final BuildingPolygonRepository buildingPolygonRepository;
    private final BuildingTypeRepository buildingTypeRepository;
    private final BuildingPolygonMapper buildingPolygonMapper;
    private final BackgroundProcessor backgroundProcessor;
    private final OllamaConnectorService ollamaConnectorService;
    private final BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository;

    public BuildingService(BuildingPolygonRepository buildingPolygonRepository, 
                BuildingTypeRepository buildingTypeRepository, 
                BackgroundProcessor backgroundProcessor,
                OllamaConnectorService ollamaConnectorService,
                BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository,
                BuildingPolygonMapper buildingPolygonMapper) {
        this.buildingPolygonRepository = buildingPolygonRepository;
        this.buildingTypeRepository = buildingTypeRepository;
        this.backgroundProcessor = backgroundProcessor;
        this.ollamaConnectorService = ollamaConnectorService;
        this.buildingPolygonEmbeddingRepository = buildingPolygonEmbeddingRepository;
        this.buildingPolygonMapper = buildingPolygonMapper;
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
        scheduleEmbeddingTask(savedEntity.getBuildingId());
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
        scheduleEmbeddingTask(savedEntity.getBuildingId());
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

    /**
     * Plant een achtergrondtaak om embeddings te genereren voor een gebouw.
     * Het maken van embeddings duurt lang, daarom doen we dit op de achtergrond.
     * 
     * We geven alleen de ID door (niet de hele entity) om race conditions te voorkomen.
     * De achtergrondtaak haalt verse data op uit de database, zodat:
     * - Lazy-loaded relaties correct worden opgehaald binnen een nieuwe transactie
     * - Er geen StaleObjectStateException optreedt door detached entities
     * 
     * @param buildingId Het UUID van het gebouw waarvoor embeddings gegenereerd moeten worden
     */
    private void scheduleEmbeddingTask(java.util.UUID buildingId) {
        backgroundProcessor.submitTask(() -> {
            // Haal verse data op binnen de achtergrondtaak (inclusief gebouwtype)
            var buildingOpt = buildingPolygonRepository.findByIdIncludingBuildingType(buildingId);
            
            // Null-check: als het gebouw ondertussen is verwijderd, slaan we de embedding over
            if (buildingOpt.isEmpty()) {
                System.out.println("Polygon " + buildingId + " niet meer gevonden, embedding wordt overgeslagen");
                return;
            }
            
            var building = buildingOpt.get();
            
            String source = new BuildingEmbeddingMapper().toEmbeddableText(building);
            float[] embedding = ollamaConnectorService.createEmbedding(source);
            String modelName = ollamaConnectorService.getEmbeddingModelName();

            BuildingPolygonEmbeddingEntity embeddingEntity = new BuildingPolygonEmbeddingEntity(
                building.getBuildingId(),
                embedding,
                modelName,
                source,
                java.time.OffsetDateTime.now()
            );

            buildingPolygonEmbeddingRepository.save(embeddingEntity);
        });
    }


    // private List<String> getBuildingsInZone(){

    // }
    
    @ToolFunctionCall(
        name = "get_buildings_based_on_description",
        description = "Get a list of building descriptions (full text) that match the given description based on embedding search. \n Useful for finding buildings that match a certain description or function."
    )
    private List<String> getBuildingsBasedOnDescription(
        @ToolParameter(description = "The fonetic search string to search the embeddings for.") String prompt, 
        @ToolParameter(description = "The maximum number of building descriptions to return.") int limit){
        float[] promptEmbedding = ollamaConnectorService.createEmbedding(prompt);
        return buildingPolygonEmbeddingRepository
            .findNearestByEmbedding(promptEmbedding, 5)
            .stream().map(x->x.getEmbeddingSource()).toList();
    }

}
