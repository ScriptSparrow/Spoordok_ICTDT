package nhl.stenden.spoordock.services;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.backgroundprocessor.BackgroundProcessor;
import nhl.stenden.spoordock.database.BuildingPolygonEmbeddingRepository;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEmbeddingEntity;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.llmService.OllamaEmbeddingClient;
import nhl.stenden.spoordock.services.mappers.BuildingEmbeddingMapper;

@Component
public class BuildingEmbeddingService {

    private final BackgroundProcessor backgroundProcessor;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository;

    public BuildingEmbeddingService(
        BackgroundProcessor backgroundProcessor,
        OllamaEmbeddingClient ollamaEmbeddingClient,
        BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository
    ){
        this.backgroundProcessor = backgroundProcessor;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.buildingPolygonEmbeddingRepository = buildingPolygonEmbeddingRepository;
    }

    public List<String> getBuildingsBasedOnDescription(String prompt, int limit){
        float[] promptEmbedding = ollamaEmbeddingClient.createEmbedding(prompt);
        return buildingPolygonEmbeddingRepository
            .findNearestByEmbedding(promptEmbedding, limit)
            .stream().map(x->x.getEmbeddingSource()).toList();
    }

    //Embedding takes a long time, hence the need to do this in the background
    //In general conversations don't start immediately after creating/updating a building, so this should be fine
    public void scheduleEmbeddingTask(BuildingPolygonEntity buildingDTO) {
        backgroundProcessor.submitTask(() -> {
            String source = new BuildingEmbeddingMapper().toEmbeddableText(buildingDTO);
            float[] embedding = ollamaEmbeddingClient.createEmbedding(source);
            String modelName = ollamaEmbeddingClient.getEmbeddingModelName();

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

}
