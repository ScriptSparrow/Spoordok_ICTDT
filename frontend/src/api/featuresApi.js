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
                featureType: b.buildingType?.buildingTypeId || null,  // UUID gebruiken i.p.v. naam
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
                    color: b.buildingType?.color || '#ffffff'  // Kleur uit database toevoegen
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
            
            throw error;
        }
    }

    /**
     * Maakt een nieuwe feature aan (gebouw of weg).
     */
    async create(feature) {
        
        if (this.useLocal) {
            this.localStore.set(feature.id, JSON.parse(JSON.stringify(feature)));
            return feature;
        }
        try {
            const isPoly = feature.geometry.type === 'Polygon';
            
            // Wegen naar /api/roads sturen
            if (!isPoly) {
                const url = `${this.baseUrl}/api/roads`;
                const body = {
                    id: feature.id,
                    roadDescription: feature.meta?.description || 'Nieuwe weg',
                    roadType: feature.meta?.typeId ? { id: feature.meta.typeId } : null,
                    coordinates: feature.geometry.coordinates.map(c => ({ x: c[0], y: c[1] }))
                };

                const response = await fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
                return feature;
            }

            const url = `${this.baseUrl}/api/buildings/building`;
            
            const body = {
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
            
            // Wegen naar /api/roads sturen
            if (!isPoly) {
                const url = `${this.baseUrl}/api/roads`;
                const body = {
                    id: id,
                    roadDescription: feature.meta?.description || 'Weg aangepast',
                    roadType: feature.meta?.typeId ? { id: feature.meta.typeId } : null,
                    coordinates: feature.geometry.coordinates.map(c => ({ x: c[0], y: c[1] }))
                };

                const response = await fetch(url, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
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
            
            throw error;
        }
    }

    /**
     * Verwijdert een feature van de kaart en uit de database.
     */
    async delete(id, isPolygon = true) {
        
        if (this.useLocal) {
            this.localStore.delete(id);
            return true;
        }
        try {
            // Wegen via /api/roads verwijderen
            if (!isPolygon) {
                const url = `${this.baseUrl}/api/roads`;
                const body = { id: id };

                const response = await fetch(url, {
                    method: 'DELETE',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
                return true;
            }

            const url = `${this.baseUrl}/api/buildings/building/${id}`;
            const response = await fetch(url, {
                method: 'DELETE'
            });
            
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
            return true;
        } catch (error) {
            
            throw error;
        }
    }
}
