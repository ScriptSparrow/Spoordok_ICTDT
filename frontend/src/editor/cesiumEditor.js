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
        console.log('CesiumEditor: Standje gewisseld naar', this.mode);
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
        window.addEventListener('keydown', (e) => {
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
        });
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
            console.warn('CesiumEditor: Kon geen positie vinden op de kaart bij', position);
            return;
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
        
        console.log('CesiumEditor: Klaar met tekenen!', { mode: this.mode, pointsCount: this.drawingPoints.length });
        
        const currentMode = this.mode;
        const points = [...this.drawingPoints];
        
        // We stoppen meteen met tekenen in de UI om re-entry te voorkomen
        this.setMode('IDLE');

        const minPoints = currentMode === 'DRAW' ? 3 : 2;
        if (points.length < minPoints) {
            const msg = `Niet genoeg punten! Klik minstens ${minPoints} keer.`;
            console.warn('CesiumEditor:', msg);
            if (this.onMessage) this.onMessage(msg, 'error');
            return;
        }

        const selectType = document.getElementById('select-type');
<<<<<<< HEAD
        let type = (selectType && selectType.value) ? selectType.value : null;
        
        // Fallback: gebruik de eerste beschikbare optie uit de dropdown
        if (!type && selectType && selectType.options.length > 0) {
            type = selectType.options[0].value;
        }
        
        // Fix: Als we een weg tekenen, moet het type altijd 'road' zijn
        if (this.mode === 'DRAW_ROAD') {
=======
        let type = selectType ? selectType.value : (currentMode === 'DRAW' ? 'housing' : 'road');
        
        // Fix: Als we een weg tekenen, moet het type altijd 'road' zijn
        if (currentMode === 'DRAW_ROAD') {
>>>>>>> origin/main
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
        console.log('CesiumEditor: Feature aangemaakt', feature);
<<<<<<< HEAD

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
=======
        
        // Koppel het juiste buildingTypeId uit de database mapping
        const buildingType = this.buildingTypes.find(t => t.labelName.toLowerCase() === type);
        if (buildingType) {
            feature.meta.typeId = buildingType.buildingTypeId;
            feature.meta.typeLabel = buildingType.labelName;
            feature.meta.costPerUnit = buildingType.costPerUnit;
            feature.meta.unit = buildingType.unit;
            feature.meta.residentsPerUnit = buildingType.residentsPerUnit;
            feature.meta.points = buildingType.points;
            feature.meta.inhabitable = buildingType.inhabitable;
        }

        // Metadata toevoegen zodat de backend niet gaat zeuren over @NotNull
>>>>>>> origin/main
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
<<<<<<< HEAD
        return 'Onbekend';
=======

        try {
            await this.execute({
                type: 'CREATE',
                feature: feature
            });

            // Selecteer em meteen even voor de feedback
            setTimeout(() => this.selectFeature(feature.id), 100);

            if (this.onMessage) this.onMessage('Feature succesvol opgeslagen!', 'success');
        } catch (err) {
            console.error('CesiumEditor: Opslaan mislukt', err);
            if (this.onMessage) this.onMessage('Opslaan mislukt. Controleer de console voor details.', 'error');
        }
>>>>>>> origin/main
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
            console.log('CesiumEditor: Feature aangeklikt', picked.id.id);
            this.selectFeature(picked.id.id);
        } else {
            if (this.selectedId) console.log('CesiumEditor: Naast de pot gepiest, selectie leeg');
            this.clearSelection();
        }
    }

    /**
     * Selecteert een feature en zet em in de spotlight.
     */
    selectFeature(id) {
        if (this.selectedId === id) return;
        
        console.log('CesiumEditor: Selecteer feature', id);
        if (this.selectedId) {
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
            console.log('CesiumEditor: Selectie gewist', this.selectedId);
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

<<<<<<< HEAD
        // Haal kleur uit feature meta (database) of val terug op TYPE_COLORS voor wegen
        const colorHex = feature.meta?.color || TYPE_COLORS[feature.featureType]?.toCssHexString() || '#ffffff';
        const baseColor = Color.fromCssColorString(colorHex);
=======
        // Gebruik kleur uit database als beschikbaar, anders fallback naar TYPE_COLORS
        let baseColor;
        if (feature.meta && feature.meta.color) {
            baseColor = Color.fromCssColorString(feature.meta.color);
        } else {
            baseColor = TYPE_COLORS[feature.featureType] || Color.WHITE;
        }
>>>>>>> origin/main
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

<<<<<<< HEAD
        // Opslaan naar de backend (apart van execute om dubbele lokale updates te voorkomen)
        this.api.update(this.selectedId, newFeature).catch(err => {
            console.error('CesiumEditor: Update naar backend mislukt', err);
            if (this.onMessage) this.onMessage('Opslaan naar server mislukt', 'error');
        });

        // Voeg toe aan undo stack zonder opnieuw apply() aan te roepen
        this.execute({
            type: 'UPDATE',
            id: this.selectedId,
            oldFeature: oldFeature,
            newFeature: newFeature
        }, false); // lokale updates en API call zijn al gedaan hierboven
=======
        try {
            // We passen de wijziging toe via execute -> apply
            await this.execute({
                type: 'UPDATE',
                id: this.selectedId,
                oldFeature: oldFeature,
                newFeature: newFeature
            });

            // Update de UI (zijbalk) omdat de meta-data veranderd kan zijn
            if (this.onSelectionChange) {
                this.onSelectionChange(newFeature);
            }
            
            console.log('CesiumEditor: Feature succesvol bijgewerkt');
        } catch (err) {
            console.error('CesiumEditor: Updaten mislukt', err);
            if (this.onMessage) this.onMessage('Aanpassen mislukt. Controleer de console.', 'error');
            
            // Zet de UI terug naar de oude staat
            this.syncEntity(oldFeature);
            if (this.onSelectionChange) {
                this.onSelectionChange(oldFeature);
            }
        }
>>>>>>> origin/main
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
            console.error('CesiumEditor: Verwijderen mislukt', err);
            if (this.onMessage) this.onMessage('Verwijderen mislukt.', 'error');
            
            // Herstel de entity op de kaart als het misging (apply heeft em al verwijderd!)
            this.syncEntity(feature);
        }
    }

    /**
     * Zorgt dat de Cesium entity klopt met onze data.
     */
    syncEntity(feature) {
        console.log('CesiumEditor: Entity synchroniseren', feature.id, feature);
        let entity = this.viewer.entities.getById(feature.id);
        if (!entity) {
            console.log('CesiumEditor: Nieuwe Cesium entity maken voor', feature.id);
            entity = this.viewer.entities.add({ id: feature.id });
        }

<<<<<<< HEAD
        // Haal kleur uit feature meta (database) of val terug op TYPE_COLORS voor wegen
        const colorHex = feature.meta?.color || TYPE_COLORS[feature.featureType]?.toCssHexString() || '#ffffff';
        const color = Color.fromCssColorString(colorHex);
=======
        // Gebruik de kleur uit de database (meta.color) als die beschikbaar is
        // Anders fallback naar de hardcoded TYPE_COLORS mapping of wit
        let color;
        if (feature.meta && feature.meta.color) {
            // Kleur uit database (hex string zoals "#f97316")
            color = Color.fromCssColorString(feature.meta.color);
        } else {
            // Fallback naar de hardcoded TYPE_COLORS mapping
            color = TYPE_COLORS[feature.featureType] || Color.WHITE;
        }
>>>>>>> origin/main

        if (feature.geometry.type === 'Polygon') {
            const flattened = feature.geometry.coordinates[0].reduce((acc, val) => acc.concat(val), []);
            
            if (flattened.length < 6 || flattened.some(v => isNaN(v))) {
                console.error('CesiumEditor: Ongeldige polygon coördinaten', flattened);
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
                console.error('CesiumEditor: Ongeldige weg coördinaten', flattened);
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
        console.log('CesiumEditor: Actie uitvoeren', { type: command.type, shouldApply });
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
        console.log('CesiumEditor: Actie toepassen', command);
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
                        console.log('CesiumEditor: ID-swap van', tempId, 'naar', savedFeature.id);
                        
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
            console.error('CesiumEditor: Toepassen mislukt', err);
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
            if (this.onMessage) this.onMessage('Niks om ongedaan te maken.', 'info');
            return;
        }

        console.log('CesiumEditor: Undo actie', cmd);
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
            console.error('CesiumEditor: Undo mislukt', err);
            if (this.onMessage) this.onMessage('Undo mislukt.', 'error');
        }
    }

    /**
     * Toch maar wel weer doen.
     */
    async redo() {
        const cmd = this.redoStack.pop();
        if (!cmd) {
            console.log('CesiumEditor: Niks om opnieuw te doen');
            if (this.onMessage) this.onMessage('Niks om opnieuw te doen.', 'info');
            return;
        }
        console.log('CesiumEditor: Redo actie', cmd);
        try {
            await this.apply(cmd);
            this.undoStack.push(cmd);
            if (this.onMessage) this.onMessage('Actie opnieuw uitgevoerd.', 'info');
        } catch (err) {
            console.error('CesiumEditor: Redo mislukt', err);
            if (this.onMessage) this.onMessage('Redo mislukt.', 'error');
        }
    }
}
