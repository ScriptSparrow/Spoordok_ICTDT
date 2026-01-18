import { 
    ScreenSpaceEventHandler, 
    ScreenSpaceEventType, 
    Cartesian3, 
    Color, 
    CallbackProperty, 
    PolygonHierarchy, 
    Entity,
    Math as CesiumMath,
    HeightReference
} from 'cesium';
import { createFeature } from './featureStore';
import { showDescriptionModal } from '../ui/descriptionModal.js';

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

        this.buildingTypes = []; // Lijst met gebouwtypes vanuit de backend

        this.initEvents();
        this.initKeyboard();
    }

    /**
     * Stelt de beschikbare gebouwtypes in en update de dropdown.
     */
    setBuildingTypes(types) {
        this.buildingTypes = types;
        this.updateTypeDropdown();
    }

    /**
     * Vult de dropdown met de types uit de database.
     */
    updateTypeDropdown() {
        const selectType = document.getElementById('select-type');
        if (!selectType || this.buildingTypes.length === 0) return;

        // We onthouden even wat er geselecteerd was
        const prevValue = selectType.value;
        selectType.innerHTML = '';

        this.buildingTypes.forEach(t => {
            const opt = document.createElement('option');
            // We gebruiken de labelName als waarde voor compatibiliteit met TYPE_COLORS
            opt.value = t.labelName.toLowerCase();
            opt.textContent = t.labelName;
            selectType.appendChild(opt);
        });

        // Wegen horen er ook bij in de UI, maar zijn geen 'BuildingType' in de backend
        const roadOpt = document.createElement('option');
        roadOpt.value = 'road';
        roadOpt.textContent = 'Weg';
        selectType.appendChild(roadOpt);

        // Probeer de vorige selectie te herstellen, anders default naar de eerste optie
        if (Array.from(selectType.options).some(o => o.value === prevValue)) {
            selectType.value = prevValue;
        } else if (selectType.options.length > 0) {
            // Gebruik de eerste beschikbare optie uit de database
            selectType.value = selectType.options[0].value;
        }
    }

    /**
     * Wisselt tussen de verschillende standen (tekenen, selecteren, etc).
     */
    setMode(mode) {
        this.mode = mode.toUpperCase();
        
        this.cleanupDrawing();
        
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
            if (this.mode === 'DRAW' || this.mode === 'DRAW_ROAD') {
                this.finishDrawing();
            }
        }, ScreenSpaceEventType.RIGHT_CLICK);

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
     * Slaat het getekende object op als feature.
     */
    async finishDrawing() {
        if (this.mode === 'IDLE') return;
        
        
        
        const currentMode = this.mode;
        const points = [...this.drawingPoints];
        
        // We stoppen meteen met tekenen in de UI om re-entry te voorkomen
        this.setMode('IDLE');

        const minPoints = currentMode === 'DRAW' ? 3 : 2;
        if (points.length < minPoints) {
            const msg = `Niet genoeg punten! Klik minstens ${minPoints} keer.`;
            
            if (this.onMessage) this.onMessage(msg, 'error');
            return;
        }

        const selectType = document.getElementById('select-type');
        let type = (selectType && selectType.value) ? selectType.value : null;
        
        // Fallback: gebruik de eerste beschikbare optie uit de dropdown
        if (!type && selectType && selectType.options.length > 0) {
            type = selectType.options[0].value;
        }
        
        // Fix: Als we een weg tekenen, moet het type altijd 'road' zijn
        if (this.mode === 'DRAW_ROAD') {
            type = 'road';
        }
        
        const geomType = currentMode === 'DRAW' ? 'Polygon' : 'LineString';
        
        const coords = points.map(p => {
            const carto = this.viewer.scene.globe.ellipsoid.cartesianToCartographic(p);
            const lon = CesiumMath.toDegrees(carto.longitude);
            const lat = CesiumMath.toDegrees(carto.latitude);
            return [lon, lat];
        });

        // Backend wil [[lon, lat], ...] voor Polygons en [lon, lat] voor LineStrings
        const finalCoords = geomType === 'Polygon' ? [[...coords, coords[0]]] : coords;

        const feature = createFeature(type, geomType, finalCoords);
        

        // Stel de typeId en kleur in vanaf de dropdown (nu een UUID)
        if (selectType && selectType.value) {
            feature.meta.typeId = selectType.value;
            // Haal de kleur uit het data-color attribuut van de geselecteerde optie
            const selectedOption = selectType.selectedOptions[0];
            if (selectedOption && selectedOption.dataset.color) {
                feature.meta.color = selectedOption.dataset.color;
            }
        }

        // Voor polygonen: toon de beschrijving modal
        if (geomType === 'Polygon') {
            // Standaard naam genereren (deze moet verplicht gewijzigd worden)
            const defaultName = `Gebouw ${feature.id.substring(0, 4)}`;
            
            // Gebouwtype naam ophalen van de geselecteerde dropdown optie
            const buildingTypeName = this.getBuildingTypeName(selectType);
            
            // Hoogte instellen vanaf de slider (moet vóór de modal opgeslagen worden)
            const heightInput = document.getElementById('height-input');
            if (heightInput) {
                feature.height = parseFloat(heightInput.value);
            }
            
            // Preview opruimen voordat de modal verschijnt
            this.cleanupDrawing();
            
            showDescriptionModal(
                feature,
                buildingTypeName,
                defaultName,
                (name, description) => {
                    // Gebruiker heeft opgeslagen - stel naam en beschrijving in
                    feature.meta.name = name;
                    feature.meta.description = description;
                    
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
     * Haalt de gebouwtype naam op van de geselecteerde dropdown optie.
     * @param {HTMLSelectElement} selectElement - De type selectie dropdown
     * @returns {string} De naam van het gebouwtype
     */
    getBuildingTypeName(selectElement) {
        if (selectElement && selectElement.selectedOptions && selectElement.selectedOptions.length > 0) {
            return selectElement.selectedOptions[0].textContent || selectElement.value;
        }
        return 'Onbekend';
    }

    /**
     * Ruimt de preview zooi op.
     */
    cleanupDrawing() {
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

        // Haal kleur uit feature meta (database) of val terug op TYPE_COLORS voor wegen
        const colorHex = feature.meta?.color || TYPE_COLORS[feature.featureType]?.toCssHexString() || '#ffffff';
        const baseColor = Color.fromCssColorString(colorHex);
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
        const newFeature = JSON.parse(JSON.stringify(oldFeature));
        Object.assign(newFeature, updates);
        
        // Als het type verandert, moeten we ook de meta-data (zoals typeId en kleur) bijwerken
        if (updates.featureType) {
            const buildingType = this.buildingTypes.find(t => t.labelName.toLowerCase() === updates.featureType);
            if (buildingType) {
                newFeature.meta = {
                    ...newFeature.meta,
                    typeId: buildingType.buildingTypeId,
                    typeLabel: buildingType.labelName,
                    costPerUnit: buildingType.costPerUnit,
                    unit: buildingType.unit,
                    residentsPerUnit: buildingType.residentsPerUnit,
                    points: buildingType.points,
                    inhabitable: buildingType.inhabitable,
                    color: buildingType.color // Kleur bijwerken zodat de polygoon de juiste kleur krijgt
                };
            } else if (updates.featureType === 'road') {
                newFeature.meta = {
                    ...newFeature.meta,
                    typeId: null,
                    typeLabel: 'Weg',
                    color: '#ffff00' // Standaard gele kleur voor wegen
                };
            }
        }

        // Opslaan naar de backend (apart van execute om dubbele lokale updates te voorkomen)
        this.api.update(this.selectedId, newFeature).catch(err => {
            
            if (this.onMessage) this.onMessage('Opslaan naar server mislukt', 'error');
        });

        // Voeg toe aan undo stack zonder opnieuw apply() aan te roepen
        this.execute({
            type: 'UPDATE',
            id: this.selectedId,
            oldFeature: oldFeature,
            newFeature: newFeature
        }, false); // lokale updates en API call zijn al gedaan hierboven
    }

    /**
     * Weg ermee! Verwijdert de geselecteerde feature.
     */
    async deleteSelected() {
        if (!this.selectedId) return;
        const feature = this.featureStore.getFeature(this.selectedId);
        
        try {
            await this.execute({
                type: 'DELETE',
                feature: JSON.parse(JSON.stringify(feature))
            });
            
            this.clearSelection();
            if (this.onMessage) this.onMessage('Feature verwijderd.', 'info');
        } catch (err) {
            
            if (this.onMessage) this.onMessage('Verwijderen mislukt.', 'error');
            
            // Herstel de entity op de kaart als het misging (apply heeft em al verwijderd!)
            this.syncEntity(feature);
        }
    }

    /**
     * Zorgt dat de Cesium entity klopt met onze data.
     */
    syncEntity(feature) {
        
        let entity = this.viewer.entities.getById(feature.id);
        if (!entity) {
            
            entity = this.viewer.entities.add({ id: feature.id });
        }

        // Haal kleur uit feature meta (database) of val terug op TYPE_COLORS voor wegen
        const colorHex = feature.meta?.color || TYPE_COLORS[feature.featureType]?.toCssHexString() || '#ffffff';
        const color = Color.fromCssColorString(colorHex);

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

            const positions = Cartesian3.fromDegreesArray(flattened);
            entity.polyline = {
                positions: positions,
                width: feature.width || 5,
                material: color.withAlpha(0.6),
                clampToGround: true
            };
            entity.polygon = undefined;
        }
    }

    /**
     * Voert een actie uit en zet em op de undo-stack.
     */
    async execute(command, shouldApply = true) {
        
        if (shouldApply) {
            await this.apply(command);
        }
        this.undoStack.push(command);
        this.redoStack = [];
    }

    /**
     * Past de actie echt toe in de store en backend.
     * 
     * PR1: Bij CREATE wordt de tijdelijke ID vervangen door de database-gegenereerde UUID.
     * De backend retourneert de echte UUID die we gebruiken om de lokale data bij te werken.
     */
    async apply(command) {
        
        try {
            switch (command.type) {
                case 'CREATE':
                    // PR1: Sla tijdelijke ID op voor latere vervanging
                    const tempId = command.feature.id;
                    this.featureStore.addFeature(command.feature);
                    this.syncEntity(command.feature);
                    
                    // PR1: Haal de opgeslagen feature op met database-gegenereerde UUID
                    const savedFeature = await this.api.create(command.feature);
                    
                    // PR1: Vervang tijdelijke ID door database-UUID als deze verschillend is
                    if (savedFeature.id && savedFeature.id !== tempId) {
                        
                        
                        // Verwijder entity en feature met tijdelijke ID
                        this.featureStore.removeFeature(tempId);
                        this.viewer.entities.removeById(tempId);
                        
                        // Update command.feature met nieuwe ID (voor undo/redo)
                        command.feature.id = savedFeature.id;
                        
                        // Voeg feature toe met database-gegenereerde UUID
                        this.featureStore.addFeature(command.feature);
                        this.syncEntity(command.feature);
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
            
            if (this.onMessage) this.onMessage('Niks om ongedaan te maken.', 'info');
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
            if (this.onMessage) this.onMessage('Actie ongedaan gemaakt.', 'info');
        } catch (err) {
            
            if (this.onMessage) this.onMessage('Undo mislukt.', 'error');
        }
    }

    /**
     * Toch maar wel weer doen.
     */
    async redo() {
        const cmd = this.redoStack.pop();
        if (!cmd) {
            
            if (this.onMessage) this.onMessage('Niks om opnieuw te doen.', 'info');
            return;
        }
        
        try {
            await this.apply(cmd);
            this.undoStack.push(cmd);
            if (this.onMessage) this.onMessage('Actie opnieuw uitgevoerd.', 'info');
        } catch (err) {
            
            if (this.onMessage) this.onMessage('Redo mislukt.', 'error');
        }
    }
}
