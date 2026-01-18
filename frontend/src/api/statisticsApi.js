/**
 * API klasse voor het ophalen van statistieken van de backend.
 */
export class StatisticsApi {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
    }

    /**
     * Haalt alle statistieken op van de backend.
     * @returns {Promise<Object>} Statistieken object met:
     *   - totalCost: Totale kosten van alle gebouwen
     *   - averageCost: Gemiddelde kosten per gebouw
     *   - averageCostPerCitizen: Gemiddelde kosten per bewoner
     *   - totalCapacity: Totale capaciteit (bewoners) van bewoonbare gebouwen
     *   - totalPoints: Totale punten van alle gebouwen
     *   - totalLiveableBuildings: Aantal bewoonbare gebouwen
     *   - tallestBuilding: Hoogte van het hoogste gebouw
     *   - lowestBuilding: Hoogte van het laagste gebouw
     *   - averageHeight: Gemiddelde hoogte van alle gebouwen
     *   - totalBuildings: Totaal aantal gebouwen
     */
    async getStatistics() {
        try {
            const response = await fetch(`${this.baseUrl}/api/statistics`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('StatisticsApi.getStatistics ging mis:', error);
            throw error;
        }
    }
}
