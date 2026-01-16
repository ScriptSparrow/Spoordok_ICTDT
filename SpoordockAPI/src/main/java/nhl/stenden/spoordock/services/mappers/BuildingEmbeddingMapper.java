package nhl.stenden.spoordock.services.mappers;

import org.locationtech.jts.geom.Polygon;

import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.llmService.EmbeddableTextMapper;

public class BuildingEmbeddingMapper implements EmbeddableTextMapper<BuildingPolygonEntity> {
    @Override
    public String toEmbeddableText(BuildingPolygonEntity building) {
        StringBuilder embeddableText = new StringBuilder()
            .append("Building ID: ").append(building.getBuildingId().toString()).append("\n")
            .append("Height: ").append(building.getHeight()).append(" meters\n");

        if (building.getBuildingType() != null) {
            embeddableText
                .append("Building Type: ").append(building.getBuildingType().getName()).append("\n")
                .append("Type Description: ").append(building.getBuildingType().getDescription()).append("\n")
                .append("Unit: ").append(building.getBuildingType().getUnit()).append("\n")
                .append("Cost per Unit: ").append(building.getBuildingType().getCostPerUnit()).append("\n")
                .append("Residents/Employees per Unit: ").append(building.getBuildingType().getResidentsPerUnit()).append("\n")
                .append("Score per Unit: ").append(building.getBuildingType().getPoints()).append("\n");
        } else {
            embeddableText.append("Building Type: Unknown\n");
        }

        embeddableText.append("Name: ").append(building.getName()).append("\n");
        embeddableText.append("Description: ").append(building.getDescription()).append("\n");

        Polygon polygon = building.getPolygon();
        embeddableText.append("Polygon Coordinates: ").append("\n")
            .append("[");

        for (int i = 0; i < polygon.getNumPoints(); i++) {
            var coord = polygon.getCoordinates()[i];
            embeddableText.append("(").append(coord.x).append(", ").append(coord.y).append(")");
            if (i < polygon.getNumPoints() - 1) {
                embeddableText.append(", ");
            }
        }   
        embeddableText.append("]");
        return embeddableText.toString();
    }

}
