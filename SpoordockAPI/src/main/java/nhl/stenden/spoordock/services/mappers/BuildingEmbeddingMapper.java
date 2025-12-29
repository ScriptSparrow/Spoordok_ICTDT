package nhl.stenden.spoordock.services.mappers;

import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.llmService.EmbeddableTextMapper;

public class BuildingEmbeddingMapper implements EmbeddableTextMapper<BuildingPolygonEntity> {
    @Override
    public String toEmbeddableText(BuildingPolygonEntity building) {
        StringBuilder embeddableText = new StringBuilder();

        embeddableText.append("Building ID: ").append(building.getBuildingId().toString()).append("\n");
        embeddableText.append("Height: ").append(building.getHeight()).append(" meters\n");

        if (building.getBuildingType() != null) {
            embeddableText.append("Building Type: ").append(building.getBuildingType().getName()).append("\n");
            embeddableText.append("Description: ").append(building.getBuildingType().getDescription()).append("\n");
        } else {
            embeddableText.append("Building Type: Unknown\n");
        }

        embeddableText.append("Name: ").append(building.getName()).append("\n");
        embeddableText.append("Description: ").append(building.getDescription()).append("\n");

        embeddableText.append("Polygon Coordinates: ").append(building.getPolygon().toString()).append("\n");

        return embeddableText.toString();
    }

}
