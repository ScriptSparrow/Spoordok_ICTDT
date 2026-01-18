package nhl.stenden.spoordock.controllers.dtos;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO voor het Gegevens panel met statistieken over alle gebouwen.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsDTO {

    // 1. Totale kosten van alle polygonen (kosten_per_eenheid * volume/area)
    private double totalCost;

    // 2. Gemiddelde kosten per gebouw
    private double averageCost;

    // 3. Gemiddelde kosten per bewoner
    private double averageCostPerCitizen;

    // 4. Totale capaciteit (bewoners) van bewoonbare gebouwen
    private double totalCapacity;

    // 5. Totale punten van alle polygonen
    private double totalPoints;

    // 6. Totaal aantal bewoonbare gebouwen
    private int totalLiveableBuildings;

    // 7. Hoogste gebouw (hoogte in meters)
    private double tallestBuilding;

    // 8. Laagste gebouw (hoogte in meters)
    private double lowestBuilding;

    // 9. Gemiddelde hoogte van alle polygonen
    private double averageHeight;

    // Extra: Totaal aantal gebouwen
    private int totalBuildings;

    // Aantal gebouwen per gebouwtype (naam -> aantal)
    private Map<String, Integer> buildingTypeCounts;
}
