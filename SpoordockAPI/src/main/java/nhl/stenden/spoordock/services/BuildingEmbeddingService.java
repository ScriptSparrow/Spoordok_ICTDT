package nhl.stenden.spoordock.services;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.backgroundprocessor.BackgroundProcessor;
import nhl.stenden.spoordock.database.BuildingPolygonEmbeddingRepository;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEmbeddingEntity;
import nhl.stenden.spoordock.llmService.OllamaEmbeddingClient;
import nhl.stenden.spoordock.services.mappers.BuildingEmbeddingMapper;

@Component
public class BuildingEmbeddingService {

    private final BackgroundProcessor backgroundProcessor;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository;
    private final BuildingPolygonRepository buildingPolygonRepository;

    public BuildingEmbeddingService(
        BackgroundProcessor backgroundProcessor,
        OllamaEmbeddingClient ollamaEmbeddingClient,
        BuildingPolygonEmbeddingRepository buildingPolygonEmbeddingRepository,
        BuildingPolygonRepository buildingPolygonRepository){
        this.backgroundProcessor = backgroundProcessor;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.buildingPolygonEmbeddingRepository = buildingPolygonEmbeddingRepository;
        this.buildingPolygonRepository = buildingPolygonRepository;
    }

    public List<String> getBuildingsBasedOnDescription(String prompt, int limit){
        float[] promptEmbedding = ollamaEmbeddingClient.createEmbedding(prompt);
        return buildingPolygonEmbeddingRepository
            .findNearestByEmbedding(promptEmbedding, limit)
            .stream().map(x->x.getEmbeddingSource()).toList();
    }

    //Embedding takes a long time, hence the need to do this in the background
    //In general conversations don't start immediately after creating/updating a building, so this should be fine
    public void scheduleEmbeddingTask(java.util.UUID buildingId) {
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
            float[] embedding = ollamaEmbeddingClient.createEmbedding(source);
            String modelName = ollamaEmbeddingClient.getEmbeddingModelName();

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

}
