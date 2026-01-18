package nhl.stenden.spoordock.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhl.stenden.spoordock.controllers.dtos.StatisticsDTO;
import nhl.stenden.spoordock.database.BuildingPolygonRepository;
import nhl.stenden.spoordock.database.entities.BuildingPolygonEntity;
import nhl.stenden.spoordock.database.entities.BuildingTypeEntity;

/**
 * Service voor het berekenen van statistieken voor het Gegevens panel.
 */
@Service
public class StatisticsService {

    private final BuildingPolygonRepository buildingPolygonRepository;

    public StatisticsService(BuildingPolygonRepository buildingPolygonRepository) {
        this.buildingPolygonRepository = buildingPolygonRepository;
    }

    /**
     * Berekent alle statistieken voor het Gegevens panel.
     *
     * @return StatisticsDTO met alle berekende statistieken
     */
    @Transactional(readOnly = true)
    public StatisticsDTO calculateStatistics() {
        // Haal alle gebouwen op inclusief hun gebouwtypes (eager load)
        List<BuildingPolygonEntity> buildings = buildingPolygonRepository.findAllIncludingBuildingType();

        if (buildings.isEmpty()) {
            return StatisticsDTO.builder()
                .totalCost(0)
                .averageCost(0)
                .averageCostPerCitizen(0)
                .totalCapacity(0)
                .totalPoints(0)
                .totalLiveableBuildings(0)
                .tallestBuilding(0)
                .lowestBuilding(0)
                .averageHeight(0)
                .totalBuildings(0)
                .buildingTypeCounts(new HashMap<>())
                .build();
        }

        // Haal oppervlaktes op via PostGIS (in vierkante meters)
        // ST_Area(punten::geography) berekent de oppervlakte correct voor WGS84 coördinaten
        Map<UUID, Double> areaMap = new HashMap<>();
        List<Object[]> areaResults = buildingPolygonRepository.findAllBuildingAreasInSquareMeters();
        for (Object[] row : areaResults) {
            UUID id = (UUID) row[0];
            Double area = ((Number) row[1]).doubleValue();
            areaMap.put(id, area);
        }

        double totalCost = 0;
        double totalCapacity = 0;
        double totalPoints = 0;
        int totalLiveableBuildings = 0;
        double totalResidents = 0;

        double tallestBuilding = Double.MIN_VALUE;
        double lowestBuilding = Double.MAX_VALUE;
        double totalHeight = 0;

        // Map voor het tellen van gebouwen per type
        Map<String, Integer> buildingTypeCounts = new HashMap<>();

        for (BuildingPolygonEntity building : buildings) {
            double height = building.getHeight();
            BuildingTypeEntity type = building.getBuildingType();

            // Tel gebouwen per type
            String typeName = type.getName();
            buildingTypeCounts.merge(typeName, 1, Integer::sum);

            // Haal oppervlakte op uit de PostGIS resultaten (in vierkante meters)
            double area = areaMap.getOrDefault(building.getBuildingId(), 0.0);
            double volume = area * height;

            // Bepaal meetwaarde op basis van eenheid (m2 of m3)
            String unit = type.getUnit() != null ? type.getUnit() : "m3";
            double measureValue = unit.equalsIgnoreCase("m2") ? area : volume;

            // Totale kosten
            double cost = type.getCostPerUnit() * measureValue;
            totalCost += cost;

            // Capaciteit en bewoners (alleen voor bewoonbare gebouwen)
            if (type.isInhabitable()) {
                Double residentsPerUnit = type.getResidentsPerUnit();
                if (residentsPerUnit != null) {
                    double residents = residentsPerUnit * measureValue;
                    totalCapacity += residents;
                    totalResidents += residents;
                }
                totalLiveableBuildings++;
            }

            // Totale punten
            Integer points = type.getPoints();
            if (points != null) {
                totalPoints += points;
            }

            // Track hoogtes voor min/max/gem
            if (height > tallestBuilding) {
                tallestBuilding = height;
            }
            if (height < lowestBuilding) {
                lowestBuilding = height;
            }
            totalHeight += height;
        }

        // Bereken afgeleide statistieken
        int totalBuildings = buildings.size();
        double averageCost = totalCost / totalBuildings;
        double averageCostPerCitizen = totalResidents > 0 ? totalCost / totalResidents : 0;
        double averageHeight = totalHeight / totalBuildings;

        // Handel edge cases af
        if (tallestBuilding == Double.MIN_VALUE) tallestBuilding = 0;
        if (lowestBuilding == Double.MAX_VALUE) lowestBuilding = 0;

        return StatisticsDTO.builder()
            .totalCost(totalCost)
            .averageCost(averageCost)
            .averageCostPerCitizen(averageCostPerCitizen)
            .totalCapacity(totalCapacity)
            .totalPoints(totalPoints)
            .totalLiveableBuildings(totalLiveableBuildings)
            .tallestBuilding(tallestBuilding)
            .lowestBuilding(lowestBuilding)
            .averageHeight(averageHeight)
            .totalBuildings(totalBuildings)
            .buildingTypeCounts(buildingTypeCounts)
            .build();
    }
}
