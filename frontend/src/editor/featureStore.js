/**
 * Store voor het beheren van alle features (gebouwen/wegen) in het geheugen.
 * Lekker makkelijk om overal bij te kunnen.
 */
export class FeatureStore {
    constructor() {
        this.features = new Map();
    }

    /**
     * Voegt een feature toe aan de map.
     */
    addFeature(feature) {
        
        this.features.set(feature.id, feature);
    }

    /**
     * Trapt een feature uit de map.
     */
    removeFeature(id) {
        
        this.features.delete(id);
    }

    /**
     * Haalt een specifieke feature op via ID.
     */
    getFeature(id) {
        return this.features.get(id);
    }

    /**
     * Update een bestaande feature met nieuwe data.
     */
    updateFeature(id, updates) {
        
        const feature = this.features.get(id);
        if (feature) {
            Object.assign(feature, updates);
        }
    }

    /**
     * Geeft een lijstje van alle features die we nu hebben.
     */
    getAllFeatures() {
        return Array.from(this.features.values());
    }
}

/**
 * Utility om een nieuw feature object te maken met een uniek ID.
 */
export function createFeature(type, geometryType, coordinates) {
    return {
        id: crypto.randomUUID(),
        featureType: type,
        height: 10,
        width: type === 'road' ? 5 : 0,
        geometry: {
            type: geometryType,
            coordinates: coordinates
        },
        meta: {}
    };
}
