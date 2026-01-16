package nhl.stenden.spoordock.services.llmTools;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.llmService.ToolHandling.ToolFunctionCall;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolParameter;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolService;
import nhl.stenden.spoordock.services.BuildingEmbeddingService;

@Component
public class BuildingLookupTool implements ToolService {

    private final BuildingEmbeddingService buildingEmbeddingService;

    public BuildingLookupTool(BuildingEmbeddingService buildingEmbeddingService) {
        this.buildingEmbeddingService = buildingEmbeddingService;
    }

    @ToolFunctionCall(name = "get_buildings_based_on_description", description = "Get a list of building descriptions (full text) with relevant data that match the given description based on embedding search. \n Useful for finding buildings that match a certain description or function.")
    public List<String> getBuildingsBasedOnDescription(
            @ToolParameter(description = "The semantic search string to search the embeddings for.") String prompt,
            @ToolParameter(description = "The maximum number of building descriptions to return.") int limit) {
        return buildingEmbeddingService.getBuildingsBasedOnDescription(prompt, limit);
    }

}
