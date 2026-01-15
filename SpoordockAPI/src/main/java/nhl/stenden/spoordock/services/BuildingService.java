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

    public BuildingPolygonDTO addBuilding(BuildingPolygonDTO buildingDTO) {
        var entity = buildingPolygonMapper.toEntity(buildingDTO);
        var savedEntity = buildingPolygonRepository.save(entity);
        scheduleEmbeddingTask(savedEntity);
        return buildingPolygonMapper.toDTO(savedEntity);
    }

    
    public BuildingPolygonDTO updateBuilding(BuildingPolygonDTO buildingDTO) {
        var entity = buildingPolygonMapper.toEntity(buildingDTO);
        var savedEntity = buildingPolygonRepository.save(entity);
        scheduleEmbeddingTask(savedEntity);
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

    //Embedding takes a long time, hence the need to do this in the background
    //In general conversations don't start immediately after creating/updating a building, so this should be fine
    private void scheduleEmbeddingTask(BuildingPolygonEntity buildingDTO) {
        backgroundProcessor.submitTask(() -> {
            String source = new BuildingEmbeddingMapper().toEmbeddableText(buildingDTO);
            float[] embedding = ollamaConnectorService.createEmbedding(source);
            String modelName = ollamaConnectorService.getEmbeddingModelName();

            BuildingPolygonEmbeddingEntity embeddingEntity = new BuildingPolygonEmbeddingEntity(
                buildingDTO.getBuildingId(),
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
    public List<String> getBuildingsBasedOnDescription(
        @ToolParameter(description = "The fonetic search string to search the embeddings for.") String prompt, 
        @ToolParameter(description = "The maximum number of building descriptions to return.") int limit){
        float[] promptEmbedding = ollamaConnectorService.createEmbedding(prompt);
        return buildingPolygonEmbeddingRepository
            .findNearestByEmbedding(promptEmbedding, 5)
            .stream().map(x->x.getEmbeddingSource()).toList();
    }

}
