

/**
 * @typedef {Object} BuidlingType
 * @property {string} buildingTypeId - Unieke ID van het gebouwtype
 * @property {string} labelName - Naam van het gebouwtype
 * @property {string} description - Beschrijving van het gebouwtype
 * @property {string} unit - Eenheid voor kostenberekening ('m2' of 'm3')
 * @property {number} costPerUnit - Kosten per eenheid
 * @property {boolean} inhabitable - Of het gebouwtype bewoonbaar is
 * @property {number|null} residentsPerUnit - Aantal bewoners per eenheid (null als niet bewoonbaar)
 * @property {number} points - Punten toegekend aan dit gebouwtype
 * @property {string} color - Kleurcode voor dit gebouwtype
 */
export const BuildingType = /** @type {BuidlingType} */ (null);

export class FeaturesApi {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
    }

    /**
     * @returns {BuidlingType[]} Lijst van gebouwtypes
     */
    async getList(){
        try {
            const response = await fetch(`${this.baseUrl}/api/features/list`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('FeaturesApi.getList ging mis:', error);
            throw error;
        }   
    }

}