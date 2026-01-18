package nhl.stenden.spoordock.services.llmTools;

import java.util.List;

import org.springframework.stereotype.Component;

import nhl.stenden.spoordock.controllers.dtos.common.Coordinate;
import nhl.stenden.spoordock.controllers.dtos.polygon.PolygonDTO;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolFunctionCall;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolParameter;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolService;
import nhl.stenden.spoordock.services.BuildingEmbeddingService;
import nhl.stenden.spoordock.services.BuildingService;

@Component
public class BuildingLookupTool implements ToolService {

    private final BuildingEmbeddingService buildingEmbeddingService;
    private final BuildingService buildingService;

    public BuildingLookupTool(BuildingEmbeddingService buildingEmbeddingService, BuildingService buildingService){
        this.buildingService = buildingService;
        this.buildingEmbeddingService = buildingEmbeddingService;
    }

    @ToolFunctionCall(name = "get_buildings_list", description = "Get a list of all building names and locations in the database.")
    public String getAllBuildingNames() {
        var buildings = buildingService.getBuildingPolygons(true);
        
        return String.join("\n-: ", buildings.stream().
            map(x -> { 
                StringBuilder builder =  new StringBuilder()
                .append("Name: ").append(x.getName()).append('\n')
                .append("Type: ").append(x.getBuildingType() != null ? x.getBuildingType().getLabelName() : "Unknown").append('\n');
                
                PolygonDTO polygon = x.getPolygon();
                builder.append("Height: ").append(x.getHeight()).append(" meters").append('\n');
                builder.append("Polygon Coordinates: ").append("\n");
                builder.append("[");
                for(Coordinate coord : polygon.getCoordinates()){
                    builder.append("(").append(coord.getX()).append(", ").append(coord.getY()).append("), ").append('\n');
                }
                builder.append("]");
                return builder.toString();
            }).toList());
    }

    @ToolFunctionCall(name = "get_buildings_based_on_description", description = "Get a list of building descriptions (full text) with relevant data that match the given description based on embedding search. \n Useful for finding buildings that match a certain description or function.")
    public String getBuildingsBasedOnDescription(
            @ToolParameter(description = "The semantic search string to search the embeddings for.") String prompt,
            @ToolParameter(description = "The maximum number of building descriptions to return.") int limit) {
        return 
            String.join("\n\n", buildingEmbeddingService.getBuildingsBasedOnDescription(prompt, limit));
    }

}
