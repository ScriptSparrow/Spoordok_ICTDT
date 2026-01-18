/**
 * Api klasse om te babbelen met de Spring Boot backend.
 * Als we in local mode draaien (geen backend), slaan we alles gewoon in het geheugen op.
 */
export class FeaturesApi {
    constructor(baseUrl = '', useLocal = false) {
        this.baseUrl = baseUrl;
        this.useLocal = useLocal;
        this.localStore = new Map(); // Tijdelijke opslag voor als de backend er niet is
    }

    /**
     * Haalt alle gebouwen en wegen op.
     */
    async getAll() {
        if (this.useLocal) {
            console.log('FeaturesApi: We draaien LOKAAL, geen zin om de server te storen');
            return Array.from(this.localStore.values());
        }
        try {

            // We proberen beide lijstjes op te halen
            const [buildings, roads] = await Promise.all([
                fetch(`${this.baseUrl}/api/buildings/list?embedTypes=true`).then(r => r.ok ? r.json() : []),
                fetch(`${this.baseUrl}/api/roads/list`).then(r => r.ok ? r.json() : [])
            ]);

            // Gebouwen even omzetten naar ons eigen formaatje
            const normalizedBuildings = buildings.map(b => ({
                id: b.buildingId,
                featureType: b.buildingType?.labelName?.toLowerCase() || 'housing',
                height: b.height,
                width: 0,
                geometry: {
                    type: 'Polygon',
                    coordinates: [b.polygon.coordinates.map(c => [c.x, c.y])]
                },
                meta: { 
                    name: b.name, 
                    description: b.description, 
                    typeId: b.buildingType?.buildingTypeId,
                    typeLabel: b.buildingType?.labelName,
                    typeDescription: b.buildingType?.description,
                    costPerUnit: b.buildingType?.costPerUnit,
                    unit: b.buildingType?.unit,
                    residentsPerUnit: b.buildingType?.residentsPerUnit,
                    points: b.buildingType?.points,
                    inhabitable: b.buildingType?.inhabitable,
                    color: b.buildingType?.color
                }
            }));

            // Wegen ook even gladstrijken
            const normalizedRoads = roads.map(r => ({
                id: r.id,
                featureType: 'road',
                height: 0,
                width: r.roadType?.standardWidth || 5,
                geometry: {
                    type: 'LineString',
                    coordinates: r.coordinates.map(c => [c.x, c.y])
                },
                meta: { description: r.roadDescription, typeId: r.roadType?.id }
            }));

            return [...normalizedBuildings, ...normalizedRoads];
        } catch (error) {
            console.error('FeaturesApi.getAll ging mis:', error);
            throw error;
        }
    }

    /**
     * Haalt alle gebouwtypes op uit de backend.
     */
    async getBuildingTypes() {
        if (this.useLocal) return [];
        try {
            const response = await fetch(`${this.baseUrl}/api/building/types/list`);
            return response.ok ? await response.json() : [];
        } catch (error) {
            console.error('FeaturesApi.getBuildingTypes faalde:', error);
            return [];
        }
    }

    /**
     * Maakt een nieuwe feature aan (gebouw of weg).
     * 
     * PR1: buildingId wordt niet meer meegestuurd - de database genereert de UUID.
     * De response bevat de door de database gegenereerde buildingId.
     */
    async create(feature) {
        console.log('FeaturesApi: Nieuwe feature aanmaken', feature);
        if (this.useLocal) {
            this.localStore.set(feature.id, JSON.parse(JSON.stringify(feature)));
            return feature;
        }
        try {
            const isPoly = feature.geometry.type === 'Polygon';
            const url = isPoly ? `${this.baseUrl}/api/buildings/building` : `${this.baseUrl}/api/roads`;
            
            let body;
            if (isPoly) {
                body = {
                    name: feature.meta.name || `Gebouw`,
                    description: feature.meta.description || 'Nieuw getekend gebouw',
                    buildingType: feature.meta.typeId ? { buildingTypeId: feature.meta.typeId } : null,
                    height: feature.height,
                    polygon: {
                        coordinates: feature.geometry.coordinates[0].map(c => ({ x: c[0], y: c[1], z: 0 }))
                    }
                };
            } else {
                body = {
                    id: feature.id,
                    roadType: { roadTypeId: 1 }, // Default road type if none provided
                    roadDescription: feature.meta.description || 'Nieuwe weg',
                    width: Math.round(feature.width || 5),
                    coordinates: feature.geometry.coordinates.map(c => ({ x: c[0], y: c[1], z: 0 }))
                };
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            
            // Road response might be a string "Road segment created successfully"
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                const data = await response.json();
                
                if (isPoly) {
                    return {
                        id: data.buildingId,
                        featureType: feature.featureType,
                        height: data.height,
                        width: feature.width,
                        geometry: {
                            type: 'Polygon',
                            coordinates: [data.polygon.coordinates.map(c => [c.x, c.y])]
                        },
                        meta: { 
                            ...feature.meta, 
                            name: data.name, 
                            description: data.description 
                        }
                    };
                }
                return data;
            } else {
                // Return original feature if response is text
                return feature;
            }
        } catch (error) {
            console.error('FeaturesApi.create faalde:', error);
            throw error;
        }
    }

    /**
     * Updatet een bestaande feature.
     */
    async update(id, feature) {
        if (this.useLocal) {
            this.localStore.set(id, JSON.parse(JSON.stringify(feature)));
            return feature;
        }
        try {
            const isPoly = feature.geometry.type === 'Polygon';
            const url = isPoly ? `${this.baseUrl}/api/buildings/building/${id}` : `${this.baseUrl}/api/roads`;
            
            let body;
            if (isPoly) {
                body = {
                    buildingId: feature.id,
                    name: feature.meta.name || `Gebouw ${feature.id.substring(0, 4)}`,
                    description: feature.meta.description || 'Gebouw aangepast',
                    buildingType: feature.meta.typeId ? { buildingTypeId: feature.meta.typeId } : null,
                    height: feature.height,
                    polygon: {
                        coordinates: feature.geometry.coordinates[0].map(c => ({ x: c[0], y: c[1], z: 0 }))
                    }
                };
            } else {
                body = {
                    id: feature.id,
                    roadType: { roadTypeId: 1 },
                    roadDescription: feature.meta.description || 'Weg aangepast',
                    width: Math.round(feature.width || 5),
                    coordinates: feature.geometry.coordinates.map(c => ({ x: c[0], y: c[1], z: 0 }))
                };
            }

            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                return await response.json();
            }
            return feature;
        } catch (error) {
            console.error('FeaturesApi.update mislukt:', error);
            throw error;
        }
    }

    /**
     * Verwijdert een feature van de kaart en uit de database.
     */
    async delete(id, isPolygon = true) {
        console.log('FeaturesApi: Feature deleten', id, { isPolygon });
        if (this.useLocal) {
            this.localStore.delete(id);
            return true;
        }
        try {
            const url = isPolygon ? `${this.baseUrl}/api/buildings/building/${id}` : `${this.baseUrl}/api/roads`;
            
            const options = {
                method: 'DELETE'
            };

            if (!isPolygon) {
                options.headers = { 'Content-Type': 'application/json' };
                options.body = JSON.stringify({ id: id });
            }

            const response = await fetch(url, options);
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            return true;
        } catch (error) {
            console.error('FeaturesApi.delete ging niet lekker:', error);
            throw error;
        }
    }
}
