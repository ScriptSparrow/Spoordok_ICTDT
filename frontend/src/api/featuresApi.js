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
                featureType: b.buildingType?.name?.toLowerCase() || 'housing',
                height: b.height,
                width: 0,
                geometry: {
                    type: 'Polygon',
                    coordinates: [b.polygon.coordinates.map(c => [c.x, c.y])]
                },
                meta: { name: b.name, description: b.description, typeId: b.buildingType?.buildingTypeId }
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
     * Maakt een nieuwe feature aan (gebouw of weg).
     */
    async create(feature) {
        console.log('FeaturesApi: Nieuwe feature aanmaken', feature);
        if (this.useLocal) {
            this.localStore.set(feature.id, JSON.parse(JSON.stringify(feature)));
            return feature;
        }
        try {
            const isPoly = feature.geometry.type === 'Polygon';
            
            // Let op: Backend heeft op dit moment blijkbaar geen POST voor roads...
            if (!isPoly) {
                console.warn('FeaturesApi: Backend heeft nog geen POST voor wegen! We doen het lokaal.');
                this.localStore.set(feature.id, JSON.parse(JSON.stringify(feature)));
                return feature;
            }

            const url = `${this.baseUrl}/api/buildings/building`;
            
            const body = {
                buildingId: feature.id,
                name: feature.meta.name || `Gebouw ${feature.id.substring(0, 4)}`,
                description: feature.meta.description || 'Nieuw getekend gebouw',
                buildingType: feature.meta.typeId ? { buildingTypeId: feature.meta.typeId } : null,
                height: feature.height,
                polygon: {
                    coordinates: feature.geometry.coordinates[0].map(c => ({ x: c[0], y: c[1], z: 0 }))
                }
            };

            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            const data = await response.json();
            return data;
        } catch (error) {
            console.error('FeaturesApi.create faalde:', error);
            throw error;
        }
    }

    /**
     * Updatet een bestaande feature.
     */
    async update(id, feature) {
        console.log('FeaturesApi: Feature updaten', id, feature);
        if (this.useLocal) {
            this.localStore.set(id, JSON.parse(JSON.stringify(feature)));
            return feature;
        }
        try {
            const isPoly = feature.geometry.type === 'Polygon';
            
            // Backend heeft ook geen PUT voor wegen blijkbaar
            if (!isPoly) {
                console.warn('FeaturesApi: Backend heeft geen PUT voor wegen! Lokaal bijgewerkt.');
                this.localStore.set(id, JSON.parse(JSON.stringify(feature)));
                return feature;
            }

            const url = `${this.baseUrl}/api/buildings/building/${id}`;
            
            const body = {
                buildingId: feature.id,
                name: feature.meta.name || `Gebouw ${feature.id.substring(0, 4)}`,
                description: feature.meta.description || 'Gebouw aangepast',
                buildingType: feature.meta.typeId ? { buildingTypeId: feature.meta.typeId } : null,
                height: feature.height,
                polygon: {
                    coordinates: feature.geometry.coordinates[0].map(c => ({ x: c[0], y: c[1], z: 0 }))
                }
            };

            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            const data = await response.json();
            return data;
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
            // Wegverwijdering ook nog niet in de backend
            if (!isPolygon) {
                console.warn('FeaturesApi: Backend kan nog geen wegen deleten! Lokaal gedaan.');
                this.localStore.delete(id);
                return true;
            }

            const url = `/api/buildings/building/${id}`;
            const response = await fetch(url, {
                method: 'DELETE'
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            return true;
        } catch (error) {
            console.error('FeaturesApi.delete ging niet lekker:', error);
            throw error;
        }
    }
}
