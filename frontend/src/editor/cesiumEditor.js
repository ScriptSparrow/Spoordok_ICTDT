import { 
    ScreenSpaceEventHandler, 
    ScreenSpaceEventType, 
    Cartesian3, 
    Cartesian2,
    Color, 
    CallbackProperty, 
    PolygonHierarchy, 
    Entity,
    Math as CesiumMath,
    HeightReference,
    LabelStyle,
    VerticalOrigin
} from 'cesium';
import { createFeature } from './featureStore';
import { showDescriptionModal } from '../ui/descriptionModal.js';

// Mapping van interne type namen naar database gebouwtype namen
const TYPE_TO_BUILDING_NAME = {
    'housing': 'Vrijstaand huis',
    'parks': 'Park/groen',
    'water': 'Park/groen',
    'office': 'Bedrijfsgebouw',
    'industry': 'Bedrijfsgebouw',
    'parking': 'Parkeerplaatsen',
    'parking_covered': 'Parkeerplaatsen overdekt',
    'apartment': 'Appartement',
    'rowhouse': 'Rijtjeswoning',
    'road': 'Wegen'
};

const TYPE_COLORS = {
    parks: Color.fromCssColorString('#34d399'),
    water: Color.fromCssColorString('#60a5fa'),
    housing: Color.fromCssColorString('#fbbf24'),
    office: Color.fromCssColorString('#a78bfa'),
    industry: Color.fromCssColorString('#9ca3af'),
    road: Color.fromCssColorString('#ffff00')
};

/**
 * De grote editor klasse die alles regelt op de Cesium globe.
 * Hier zit de logica voor tekenen, selecteren en undo/redo.
 */
export class CesiumEditor {
    constructor(viewer, featureStore, api, onSelectionChange) {
        this.viewer = viewer;
        this.featureStore = featureStore;
        this.api = api;
        this.onSelectionChange = onSelectionChange;
        
        this.mode = 'IDLE'; // Mogelijke standen: 'IDLE' | 'DRAW' | 'DRAW_ROAD' | 'EDIT'
        this.selectedId = null;
        
        this.handler = new ScreenSpaceEventHandler(viewer.scene.canvas);
        this.drawingPoints = [];
        this.previewEntity = null;
        
        this.undoStack = [];
        this.redoStack = [];
        
        // Distance labels voor het weergeven van lijnlengtes tijdens tekenen
        this.distanceLabels = [];
        this.currentSegmentLabel = null;

        this.initEvents();
        this.initKeyboard();
    }

    /**
     * Wisselt tussen de verschillende standen (tekenen, selecteren, etc).
     */
    setMode(mode) {
        this.mode = mode.toUpperCase();
        
        this.cleanupDrawing();
        
        // Notify listeners about mode change
        if (this.onModeChange) this.onModeChange(this.mode);
        
        if (this.mode === 'IDLE') {
            this.clearSelection();
            this.viewer.canvas.style.cursor = 'default';
        } else if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
            this.viewer.canvas.style.cursor = 'crosshair';
            
            // Zet het type alvast goed in de dropdown als we een weg gaan tekenen
            const selectType = document.getElementById('select-type');
            if (this.mode === 'DRAW_ROAD' && selectType) {
                selectType.value = 'road';
            }
        } else {
            this.viewer.canvas.style.cursor = 'pointer';
        }
    }

    /**
     * Luistert naar muisklikken en bewegingen op de kaart.
     */
    initEvents() {
        this.handler.setInputAction((click) => {
            if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                this.addDrawingPoint(click.position);
            } else {
                this.pickFeature(click.position);
            }
        }, ScreenSpaceEventType.LEFT_CLICK);

        this.handler.setInputAction((move) => {
            if ((this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') && this.drawingPoints.length > 0) {
                this.updatePreview(move.endPosition);
            }
        }, ScreenSpaceEventType.MOUSE_MOVE);

        this.handler.setInputAction(() => {
            console.log('RIGHT_DOWN detected, mode:', this.mode);  // Debug logging
            if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                this.finishDrawing();
            }
        }, ScreenSpaceEventType.RIGHT_DOWN);

        this.handler.setInputAction(() => {
            if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                this.finishDrawing();
            }
        }, ScreenSpaceEventType.LEFT_DOUBLE_CLICK);
    }

    /**
     * Handige sneltoetsen zoals ESC en Ctrl+Z.
     */
    initKeyboard() {
        this.activeKeys = new Set();
        this.rotationInterval = null;

        window.addEventListener('keydown', (e) => {
            this.activeKeys.add(e.key);

            if (e.key === 'Escape') {
                if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                    this.setMode('IDLE');
                }
            } else if (e.key === 'Enter') {
                if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                    this.finishDrawing();
                }
            } else if (e.ctrlKey && e.key.toLowerCase() === 'z') {
                if (e.shiftKey) this.redo();
                else this.undo();
            } else if (e.ctrlKey && e.key.toLowerCase() === 'y') {
                this.redo();
            }

            this.handleRotation();
        });

        window.addEventListener('keyup', (e) => {
            this.activeKeys.delete(e.key);
            this.handleRotation();
            
            // On key release of rotation keys, persist once
            if (['a', 'd', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
                this.persistRotation();
            }
        });
    }

    /**
     * Start of stopt de continue rotatie op basis van ingedrukte toetsen.
     */
    handleRotation() {
        const rotateLeft = this.activeKeys.has('a') || this.activeKeys.has('A') || this.activeKeys.has('ArrowLeft');
        const rotateRight = this.activeKeys.has('d') || this.activeKeys.has('D') || this.activeKeys.has('ArrowRight');

        if ((rotateLeft || rotateRight) && !(rotateLeft && rotateRight)) {
            if (!this.rotationInterval) {
                this.startRotation(rotateLeft ? -1 : 1);
            } else {
                // Als we al aan het roteren waren, maar de richting is veranderd
                this.stopRotation();
                this.startRotation(rotateLeft ? -1 : 1);
            }
        } else {
            this.stopRotation();
        }
    }

    /**
     * Start het interval voor continue rotatie.
     */
    startRotation(direction) {
        if (this.mode !== 'EDIT' || !this.selectedId) return;

        const rotationSpeed = 2.0; // Graden per frame (ongeveer)
        
        this.rotationInterval = setInterval(() => {
            if (this.mode !== 'EDIT' || !this.selectedId) {
                this.stopRotation();
                return;
            }
            this.rotateFeature(this.selectedId, direction * rotationSpeed);
        }, 16); // ~60fps
    }

    /**
     * Stopt de rotatie.
     */
    stopRotation() {
        if (this.rotationInterval) {
            clearInterval(this.rotationInterval);
            this.rotationInterval = null;
        }
    }

    /**
     * Roteert een feature om zijn middelpunt.
     */
    rotateFeature(id, angleDegrees) {
        const feature = this.featureStore.getFeature(id);
        if (!feature) return;

        const angleRad = CesiumMath.toRadians(angleDegrees);
        
        // Bereken middelpunt (simpele gemiddelde van coördinaten)
        let coords;
        if (feature.geometry.type === 'Polygon') {
            coords = feature.geometry.coordinates[0];
        } else if (feature.geometry.type === 'LineString') {
            coords = feature.geometry.coordinates;
        } else {
            return;
        }

        const center = coords.reduce((acc, curr) => [acc[0] + curr[0], acc[1] + curr[1]], [0, 0])
                             .map(v => v / coords.length);

        // Roteer elk punt
        const newCoords = coords.map(p => {
            const dx = p[0] - center[0];
            const dy = p[1] - center[1];
            
            // We moeten rekening houden met de breedtegraad voor een meer 'natuurlijke' rotatie op een bol,
            // maar voor kleine gebouwen werkt simpele 2D rotatie op lon/lat vaak acceptabel genoeg.
            // Voor betere precisie zouden we naar Cartesian3 moeten en terug, maar dat is complexer.
            const cos = Math.cos(angleRad);
            const sin = Math.sin(angleRad);
            
            return [
                center[0] + dx * cos - dy * sin,
                center[1] + dx * sin + dy * cos
            ];
        });

        const updates = {
            geometry: {
                ...feature.geometry,
                coordinates: feature.geometry.type === 'Polygon' ? [newCoords] : newCoords
            }
        };

        // Update lokaal (store + entity) zonder backend call (voor de snelheid tijdens hold)
        this.featureStore.updateFeature(id, updates);
        this.syncEntity(this.featureStore.getFeature(id));
    }

    /**
     * Slaat de huidige staat op in de backend.
     */
    async persistRotation() {
        if (!this.selectedId) return;
        const feature = this.featureStore.getFeature(this.selectedId);
        if (!feature) return;

        try {
            await this.api.update(this.selectedId, feature);
        } catch (err) {
            
        }
    }

    /**
     * Berekent de afstand tussen twee punten en geeft een leesbare string terug.
     */
    calculateDistance(point1, point2) {
        const distance = Cartesian3.distance(point1, point2);
        if (distance >= 1000) {
            return (distance / 1000).toFixed(2) + ' km';
        }
        return distance.toFixed(1) + ' m';
    }

    /**
     * Berekent het middelpunt tussen twee punten.
     */
    getMidpoint(point1, point2) {
        return new Cartesian3(
            (point1.x + point2.x) / 2,
            (point1.y + point2.y) / 2,
            (point1.z + point2.z) / 2
        );
    }

    /**
     * Voegt een punt toe tijdens het tekenen.
     */
    addDrawingPoint(position) {
        let cartesian = this.viewer.scene.pickPosition(position);
        if (!cartesian) {
            // Als pickPosition faalt (bijv. geen diepte), pakken we de ellipsoid
            cartesian = this.viewer.camera.pickEllipsoid(position, this.viewer.scene.globe.ellipsoid);
        }
        
        if (!cartesian) {
            
            return;
        }
        
        // Als we minstens één vorig punt hebben, maak een label voor het voltooide segment
        if (this.drawingPoints.length > 0) {
            const prevPoint = this.drawingPoints[this.drawingPoints.length - 1];
            const midpoint = this.getMidpoint(prevPoint, cartesian);
            const distanceText = this.calculateDistance(prevPoint, cartesian);
            
            const label = this.viewer.entities.add({
                position: midpoint,
                label: {
                    text: distanceText,
                    font: '14px sans-serif',
                    fillColor: Color.WHITE,
                    style: LabelStyle.FILL,
                    showBackground: true,
                    backgroundColor: Color.BLACK.withAlpha(0.7),
                    backgroundPadding: new Cartesian2(7, 5),
                    verticalOrigin: VerticalOrigin.BOTTOM,
                    pixelOffset: new Cartesian2(0, -10),
                    disableDepthTestDistance: Number.POSITIVE_INFINITY
                }
            });
            this.distanceLabels.push(label);
        }
        
        console.log('CesiumEditor: Puntje gezet', cartesian);
        this.drawingPoints.push(cartesian);
        
        if (!this.previewEntity) {
            this.createPreviewEntity();
        }
    }

    /**
     * Maakt een tijdelijke versie van wat je aan het tekenen bent.
     */
    createPreviewEntity() {
        if (this.mode === 'DRAW') {
            this.previewEntity = this.viewer.entities.add({
                polygon: {
                    hierarchy: new CallbackProperty(() => new PolygonHierarchy(this.drawingPoints), false),
                    material: Color.WHITE.withAlpha(0.4)
                },
                polyline: {
                    positions: new CallbackProperty(() => [...this.drawingPoints, this.drawingPoints[0]], false),
                    width: 2,
                    material: Color.WHITE
                }
            });
        } else {
            this.previewEntity = this.viewer.entities.add({
                polyline: {
                    positions: new CallbackProperty(() => this.drawingPoints, false),
                    width: 4,
                    material: Color.YELLOW.withAlpha(0.6)
                }
            });
        }
    }

    /**
     * Update de preview terwijl je de muis beweegt.
     */
    updatePreview(position) {
        let cartesian = this.viewer.scene.pickPosition(position);
        if (!cartesian) {
            cartesian = this.viewer.camera.pickEllipsoid(position, this.viewer.scene.globe.ellipsoid);
        }
        
        if (!cartesian || !this.previewEntity) return;

        const points = [...this.drawingPoints, cartesian];
        
        // Update of maak het huidige segment label (volgt de muis)
        if (this.drawingPoints.length > 0) {
            const lastPoint = this.drawingPoints[this.drawingPoints.length - 1];
            const midpoint = this.getMidpoint(lastPoint, cartesian);
            const distanceText = this.calculateDistance(lastPoint, cartesian);
            
            if (!this.currentSegmentLabel) {
                this.currentSegmentLabel = this.viewer.entities.add({
                    position: midpoint,
                    label: {
                        text: distanceText,
                        font: '14px sans-serif',
                        fillColor: Color.YELLOW,
                        style: LabelStyle.FILL,
                        showBackground: true,
                        backgroundColor: Color.BLACK.withAlpha(0.7),
                        backgroundPadding: new Cartesian2(7, 5),
                        verticalOrigin: VerticalOrigin.BOTTOM,
                        pixelOffset: new Cartesian2(0, -10),
                        disableDepthTestDistance: Number.POSITIVE_INFINITY
                    }
                });
            } else {
                this.currentSegmentLabel.position = midpoint;
                this.currentSegmentLabel.label.text = distanceText;
            }
        }
        
        if (this.mode === 'DRAW') {
            if (this.previewEntity.polygon) {
                this.previewEntity.polygon.hierarchy = new PolygonHierarchy(points);
            }
            if (this.previewEntity.polyline) {
                this.previewEntity.polyline.positions = [...points, points[0]];
            }
        } else {
            if (this.previewEntity.polyline) {
                this.previewEntity.polyline.positions = points;
            }
        }
    }

    /**
     * Haalt de gebouwtype naam op voor de voorbeeldbeschrijvingen.
     * @param {string} type - Het interne type (housing, parks, etc.)
     * @returns {string} De database gebouwtype naam
     */
    getBuildingTypeName(type) {
        return TYPE_TO_BUILDING_NAME[type] || 'Vrijstaand huis';
    }

    /**
     * Slaat het getekende object op als feature.
     */
    finishDrawing() {
        console.log('CesiumEditor: Klaar met tekenen!', { mode: this.mode, pointsCount: this.drawingPoints.length });
        const minPoints = this.mode === 'DRAW' ? 3 : 2;
        if (this.drawingPoints.length < minPoints) {
            const msg = `Niet genoeg punten! Klik minstens ${minPoints} keer.`;
            
            if (this.onMessage) this.onMessage(msg, 'error');
            this.setMode('IDLE');
            return;
        }

        const selectType = document.getElementById('select-type');
        
        // Haal het geselecteerde gebouwtype op (UUID als value, labelName als tekst)
        const typeId = selectType ? selectType.value : null;
        const selectedOption = selectType && selectType.selectedOptions.length > 0 ? selectType.selectedOptions[0] : null;
        const buildingTypeLabelName = selectedOption ? selectedOption.textContent : null;
        
        // Gebruik de geselecteerde typeId als featureType, met fallback naar eerste dropdown optie
        let type = null;
        if (this.mode === 'DRAW_ROAD') {
            // Wegen hebben een apart type
            type = 'road';
        } else if (typeId) {
            // Gebruik de geselecteerde UUID uit de dropdown
            type = typeId;
        } else if (selectType && selectType.options.length > 0) {
            // Fallback: eerste beschikbare optie uit de dropdown
            type = selectType.options[0].value;
        }
        
        const geomType = this.mode === 'DRAW' ? 'Polygon' : 'LineString';
        
        const coords = this.drawingPoints.map(p => {
            const carto = this.viewer.scene.globe.ellipsoid.cartesianToCartographic(p);
            const lon = CesiumMath.toDegrees(carto.longitude);
            const lat = CesiumMath.toDegrees(carto.latitude);
            return [lon, lat];
        });

        // Backend wil [[lon, lat], ...] voor Polygons en [lon, lat] voor LineStrings
        const finalCoords = geomType === 'Polygon' ? [[...coords, coords[0]]] : coords;

        const feature = createFeature(type, geomType, finalCoords);
        
        // Stel de typeId (UUID) in voor de backend
        if (typeId) {
            feature.meta.typeId = typeId;
        }
        
        // Haal de kleur uit de geselecteerde dropdown optie en stel deze in
        if (selectedOption?.dataset?.color) {
            feature.meta.color = selectedOption.dataset.color;
        }
        
        console.log('CesiumEditor: Feature aangemaakt', feature);

        // Voor polygonen: toon de beschrijving modal
        if (geomType === 'Polygon') {
            // Standaard naam genereren (deze moet verplicht gewijzigd worden)
            const defaultName = `Gebouw ${feature.id.substring(0, 4)}`;
            
            // Gebruik de labelName van de dropdown voor voorbeeldbeschrijvingen
            const buildingTypeName = buildingTypeLabelName || 'Vrijstaand huis';
            
            showDescriptionModal(
                feature,
                buildingTypeName,
                defaultName,
                (name, description) => {
                    // Gebruiker heeft opgeslagen - stel naam en beschrijving in
                    feature.meta.name = name;
                    feature.meta.description = description;
                    
                    // Hoogte instellen vanaf de slider
                    const heightInput = document.getElementById('height-input');
                    if (heightInput) {
                        feature.height = parseFloat(heightInput.value);
                    }
                    
                    this.execute({ type: 'CREATE', feature: feature });
                    setTimeout(() => this.selectFeature(feature.id), 100);
                    if (this.onMessage) this.onMessage('Feature succesvol opgeslagen!', 'success');
                    this.setMode('IDLE');
                },
                () => {
                    // Gebruiker heeft geannuleerd - opruimen zonder op te slaan
                    if (this.onMessage) this.onMessage('Tekenen geannuleerd', 'info');
                    this.setMode('IDLE');
                }
            );
        } else {
            // Wegen - bestaand gedrag behouden
            feature.meta.description = 'Nieuwe weg';
            
            // Breedte instellen vanaf de slider
            const heightInput = document.getElementById('height-input');
            if (heightInput) {
                feature.width = parseFloat(heightInput.value);
            }
            
            this.execute({ type: 'CREATE', feature: feature });
            setTimeout(() => this.selectFeature(feature.id), 100);
            if (this.onMessage) this.onMessage('Feature succesvol opgeslagen!', 'success');
            this.setMode('IDLE');
        }
    }

    /**
     * Ruimt de preview zooi op.
     */
    cleanupDrawing() {
        // Verwijder alle afstand labels
        this.distanceLabels.forEach(label => {
            this.viewer.entities.remove(label);
        });
        this.distanceLabels = [];
        
        // Verwijder het huidige segment label
        if (this.currentSegmentLabel) {
            this.viewer.entities.remove(this.currentSegmentLabel);
            this.currentSegmentLabel = null;
        }
        
        if (this.previewEntity) {
            this.viewer.entities.remove(this.previewEntity);
            this.previewEntity = null;
        }
        this.drawingPoints = [];
    }

    /**
     * Kijkt of je op een feature klikt op de kaart.
     */
    pickFeature(position) {
        const picked = this.viewer.scene.pick(position);
        if (picked && picked.id instanceof Entity) {
            
            this.selectFeature(picked.id.id);
        } else {
            if (this.selectedId) 
            this.clearSelection();
        }
    }

    /**
     * Selecteert een feature en zet em in de spotlight.
     */
    selectFeature(id) {
        if (this.selectedId === id) return;
        
        
        if (this.selectedId) {
            this.stopRotation();
            this.persistRotation();
            this.updateEntityHighlight(this.selectedId, false);
        }
        this.selectedId = id;
        this.updateEntityHighlight(id, true);
        this.onSelectionChange(this.featureStore.getFeature(id));
    }

    /**
     * Maakt de selectie leeg.
     */
    clearSelection() {
        if (this.selectedId) {
            
            this.stopRotation();
            this.persistRotation();
            this.updateEntityHighlight(this.selectedId, false);
        }
        this.selectedId = null;
        this.onSelectionChange(null);
    }

    /**
     * Geeft de geselecteerde feature een kleurtje/highlight.
     */
    updateEntityHighlight(id, highlighted) {
        const entity = this.viewer.entities.getById(id);
        if (!entity) return;

        const feature = this.featureStore.getFeature(id);
        if (!feature) return;

        // Gebruik kleur uit meta (database) of fallback naar TYPE_COLORS of wit
        const colorHex = feature.meta?.color;
        const baseColor = colorHex ? Color.fromCssColorString(colorHex) : (TYPE_COLORS[feature.featureType] || Color.WHITE);
        const color = highlighted ? baseColor.withAlpha(0.9) : baseColor.withAlpha(0.6);

        if (entity.polygon) {
            entity.polygon.material = color;
            entity.polygon.outlineColor = highlighted ? Color.WHITE : Color.BLACK;
            entity.polygon.outlineWidth = highlighted ? 3 : 1;
        } else if (entity.polyline) {
            entity.polyline.material = color;
            entity.polyline.width = highlighted ? (feature.width || 5) + 3 : (feature.width || 5);
        }
    }

    /**
     * Update de eigenschappen van de geselecteerde feature.
     */
    async updateSelectedFeature(updates) {
        if (!this.selectedId) return;
        const oldFeature = JSON.parse(JSON.stringify(this.featureStore.getFeature(this.selectedId)));
        const newFeature = { ...oldFeature, ...updates };
        
        // Optimistisch updaten: eerst in de UI, dan pas de backend storen
        this.featureStore.updateFeature(this.selectedId, newFeature);
        this.syncEntity(newFeature);
        this.updateEntityHighlight(this.selectedId, true);

        // Direct API call om de wijzigingen naar de backend te sturen
        // (execute met shouldApply=false slaat de apply() over, dus we moeten hier de API aanroepen)
        try {
            await this.api.update(this.selectedId, newFeature);
            console.log('CesiumEditor: Feature succesvol bijgewerkt in de database');
        } catch (err) {
            console.error('CesiumEditor: Opslaan naar backend mislukt', err);
            if (this.onMessage) this.onMessage('Opslaan mislukt - wijziging alleen lokaal opgeslagen', 'error');
        }

        this.execute({
            type: 'UPDATE',
            id: this.selectedId,
            oldFeature: oldFeature,
            newFeature: newFeature
        }, false); // niet opnieuw applyen, we hebben het net gedaan
    }

    /**
     * Weg ermee! Verwijdert de geselecteerde feature.
     */
    deleteSelected() {
        if (!this.selectedId) return;
        const feature = this.featureStore.getFeature(this.selectedId);
        
        this.execute({
            type: 'DELETE',
            feature: JSON.parse(JSON.stringify(feature))
        });
        
        this.clearSelection();
    }

    /**
     * Zorgt dat de Cesium entity klopt met onze data.
     */
    syncEntity(feature) {
        
        let entity = this.viewer.entities.getById(feature.id);
        if (!entity) {
            
            entity = this.viewer.entities.add({ id: feature.id });
        }

        // Gebruik kleur uit meta (database) of fallback naar TYPE_COLORS of wit
        const colorHex = feature.meta?.color;
        const color = colorHex ? Color.fromCssColorString(colorHex) : (TYPE_COLORS[feature.featureType] || Color.WHITE);

        if (feature.geometry.type === 'Polygon') {
            const flattened = feature.geometry.coordinates[0].reduce((acc, val) => acc.concat(val), []);
            
            if (flattened.length < 6 || flattened.some(v => isNaN(v))) {
                
                return;
            }

            const positions = Cartesian3.fromDegreesArray(flattened);
            entity.polygon = {
                hierarchy: new PolygonHierarchy(positions),
                material: color.withAlpha(0.6),
                height: 0,
                heightReference: HeightReference.RELATIVE_TO_GROUND,
                extrudedHeight: feature.height || 10,
                extrudedHeightReference: HeightReference.RELATIVE_TO_GROUND,
                outline: true,
                outlineColor: Color.BLACK
            };
            entity.polyline = undefined;
        } else if (feature.geometry.type === 'LineString') {
            const flattened = feature.geometry.coordinates.reduce((acc, val) => acc.concat(val), []);
            
            if (flattened.length < 4 || flattened.some(v => isNaN(v))) {
                
                return;
            }
            console.log('width: ' + feature.width);
            const positions = Cartesian3.fromDegreesArray(flattened);
            entity.corridor = {
                positions: positions,
                width: feature.width || 5, // width in meters
                material: color.withAlpha(0.6),
                heightReference: HeightReference.CLAMP_TO_GROUND
            };
            entity.polyline = undefined;
            entity.polygon = undefined;
        }
    }

    /**
     * Voert een actie uit en zet em op de undo-stack.
     */
    async execute(command, shouldApply = true) {
        console.log('CesiumEditor: Actie uitvoeren', { type: command.type, shouldApply });
        if (shouldApply) await this.apply(command);
        this.undoStack.push(command);
        this.redoStack = [];
        
        // Always trigger onFeatureChange, even when shouldApply is false
        if (this.onFeatureChange) this.onFeatureChange(command.type);
    }

    /**
     * Past de actie echt toe in de store en backend.
     */
    async apply(command) {
        
        try {
            switch (command.type) {
                case 'CREATE':
                    this.featureStore.addFeature(command.feature);
                    this.syncEntity(command.feature);
                    const result = await this.api.create(command.feature);
                    // Sync the backend-generated ID with the local feature
                    if (result && result.buildingId && result.buildingId !== command.feature.id) {
                        const oldId = command.feature.id;
                        command.feature.id = result.buildingId;
                        this.featureStore.removeFeature(oldId);
                        this.featureStore.addFeature(command.feature);
                        this.viewer.entities.removeById(oldId);
                        this.syncEntity(command.feature);
                        console.log('CesiumEditor: ID gesynchroniseerd van', oldId, 'naar', result.buildingId);
                    }
                    break;
                case 'UPDATE':
                    this.featureStore.updateFeature(command.id, command.newFeature);
                    this.syncEntity(command.newFeature);
                    await this.api.update(command.id, command.newFeature);
                    break;
                case 'DELETE':
                    const isPolygon = command.feature.geometry.type === 'Polygon';
                    this.featureStore.removeFeature(command.feature.id);
                    this.viewer.entities.removeById(command.feature.id);
                    await this.api.delete(command.feature.id, isPolygon);
                    break;
            }
        } catch (err) {
            
            // We gooien de error door zodat de UI kan reageren
            throw err;
        }
    }

    /**
     * Oeps, teruggedraaid!
     */
    async undo() {
        const cmd = this.undoStack.pop();
        if (!cmd) {
            console.log('CesiumEditor: Niks meer ongedaan te maken');
            return;
        }

        
        try {
            switch (cmd.type) {
                case 'CREATE':
                    this.featureStore.removeFeature(cmd.feature.id);
                    this.viewer.entities.removeById(cmd.feature.id);
                    await this.api.delete(cmd.feature.id, cmd.feature.geometry.type === 'Polygon');
                    break;
                case 'UPDATE':
                    this.featureStore.updateFeature(cmd.id, cmd.oldFeature);
                    this.syncEntity(cmd.oldFeature);
                    await this.api.update(cmd.id, cmd.oldFeature);
                    break;
                case 'DELETE':
                    this.featureStore.addFeature(cmd.feature);
                    this.syncEntity(cmd.feature);
                    await this.api.create(cmd.feature);
                    break;
            }
            this.redoStack.push(cmd);
            this.clearSelection();
        } catch (err) {
            console.error('CesiumEditor: Undo mislukt', err);
        }
    }

    /**
     * Toch maar wel weer doen.
     */
    async redo() {
        const cmd = this.redoStack.pop();
        if (!cmd) {
            console.log('CesiumEditor: Niks om opnieuw te doen');
            return;
        }
        console.log('CesiumEditor: Redo actie', cmd);
        await this.apply(cmd);
        this.undoStack.push(cmd);
    }
}
