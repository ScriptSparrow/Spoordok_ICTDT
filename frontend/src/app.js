import { setupViewer, flyToSpoordok } from './cesium/viewer.js';
import { FeatureStore } from './editor/featureStore.js';
import { CesiumEditor } from './editor/cesiumEditor.js';
import { FeaturesApi } from './api/featuresApi.js';
import { FeaturesApi as BuildingTypesApi } from './api/buildTypeApi.js';
import { showEditModal } from './ui/descriptionModal.js';

// Module-level referenties zodat updateUI() toegang heeft tot de editor
let editorInstance = null;
let featureStoreInstance = null;
let buildingTypesCache = [];  // Cache voor gebouwtypes met kleuren

/**
 * Haalt de kleur op voor een gebouwtype UUID.
 * @param {string} typeId - De UUID van het gebouwtype
 * @returns {string|null} De kleurcode of null als niet gevonden
 */
function getBuildingTypeColor(typeId) {
    const buildingType = buildingTypesCache.find(t => t.buildingTypeId === typeId);
    return buildingType?.color || null;
}

/**
 * De main entry point van onze app.
 * Hier knopen we alles aan elkaar: de viewer, de store en de UI.
 */
async function init() {
    const featureStore = new FeatureStore();
    featureStoreInstance = featureStore;
    
    // We kijken of we lokaal moeten draaien (handig voor testen zonder backend)
    const useLocal = false;
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
    const api = new FeaturesApi(apiBaseUrl, useLocal);
    const buildingTypesApi = new BuildingTypesApi(apiBaseUrl);
    
    // Cesium viewer opstarten
    const viewer = await setupViewer('cesiumContainer');
    
    // Onze eigen editor initialiseren
    const editor = new CesiumEditor(viewer, featureStore, api, (selected) => {
        updateUI(selected);
    });
    editorInstance = editor;

    // Callback voor berichtjes naar de gebruiker
    editor.onMessage = (msg, type) => showToast(msg, type);

    // Knoppen uit de HTML vissen en acties koppelen
    const btnDraw = document.getElementById('btn-draw');
    const btnRoad = document.getElementById('btn-road');
    const btnEdit = document.getElementById('btn-edit');
    const btnDelete = document.getElementById('btn-delete');
    const btnUndo = document.getElementById('btn-undo');
    const btnRedo = document.getElementById('btn-redo');
    const btnReset = document.getElementById('btn-reset-camera');

    const selectType = document.getElementById('select-type');
    const heightRange = document.getElementById('height-range');
    const heightInput = document.getElementById('height-input');

    if (btnDraw) btnDraw.onclick = () => editor.setMode('DRAW');
    if (btnRoad) btnRoad.onclick = () => editor.setMode('DRAW_ROAD');
    if (btnEdit) btnEdit.onclick = () => editor.setMode('EDIT');
    
    if (btnDelete) btnDelete.onclick = () => {
        editor.deleteSelected();
        showToast('Feature is de prullenbak in gegaan');
    };
    
    if (btnUndo) btnUndo.onclick = () => editor.undo();
    if (btnRedo) btnRedo.onclick = () => editor.redo();
    
    if (btnReset) btnReset.onclick = () => flyToSpoordok(viewer);

    // Type aanpassen als je de dropdown verandert (value is nu een UUID)
    if (selectType) {
        selectType.onchange = (e) => {
            const newTypeId = e.target.value;  // UUID van het gebouwtype
            // Haal de huidige feature op om meta properties te behouden
            const currentFeature = featureStore.getFeature(editor.selectedId);
            if (currentFeature) {
                // Haal de kleur op voor het nieuwe gebouwtype
                const newColor = getBuildingTypeColor(newTypeId);
                
                // Update zowel typeId ALS color in meta
                const updatedMeta = { 
                    ...currentFeature.meta, 
                    typeId: newTypeId,
                    color: newColor  // Kleur ook bijwerken voor directe visuele feedback
                };
                
                editor.updateSelectedFeature({ 
                    featureType: newTypeId,  // Voor consistentie
                    meta: updatedMeta 
                });
                
                
            }
        };
    }

    // Hoogte of breedte aanpassen met de slider of het getal-vakje
    const handleHeightChange = (e) => {
        const val = parseFloat(e.target.value);
        heightRange.value = val;
        heightInput.value = val;
        
        const selected = featureStore.getFeature(editor.selectedId);
        if (selected) {
            if (selected.geometry.type === 'Polygon') {
                editor.updateSelectedFeature({ height: val });
            } else {
                editor.updateSelectedFeature({ width: val });
            }
        }
    };

    if (heightRange) heightRange.oninput = handleHeightChange;
    if (heightInput) heightInput.oninput = handleHeightChange;

    // Gebouwtypes ophalen en dropdown vullen
    try {
        const buildingTypes = await buildingTypesApi.getList();
        buildingTypesCache = buildingTypes;  // Bewaar voor kleur-lookup bij type wijziging
        
        // Leeg de placeholder optie
        if (selectType) {
            selectType.innerHTML = '';
            
            // Vul met gebouwtypes uit de database (filter "Wegen" eruit)
            buildingTypes
                .filter(type => type.labelName !== 'Wegen')
                .forEach(type => {
                    const option = document.createElement('option');
                    option.value = type.buildingTypeId;  // UUID als value
                    option.textContent = type.labelName;  // Naam als tekst
                    selectType.appendChild(option);
                });
        }
    } catch (e) {
        
        // Als het niet lukt, laat de placeholder staan
    }

    // Alles ophalen uit de backend (of lokale store) bij het opstarten
    try {
        const features = await api.getAll();
        features.forEach(f => {
            featureStore.addFeature(f);
            editor.syncEntity(f);
        });
        
    } catch (e) {
        
        showToast('Server niet bereikbaar - we slaan het lokaal voor je op', 'error');
    }

    // Camera vliegen naar Spoordok
    flyToSpoordok(viewer);
}

/**
 * Update de info in de rechter zijbalk.
 */
function updateUI(selected) {
    const infoContent = document.getElementById('info-content');
    const selectType = document.getElementById('select-type');
    const heightRange = document.getElementById('height-range');
    const heightInput = document.getElementById('height-input');

    if (!infoContent) return;

    if (selected) {
        const isPoly = selected.geometry.type === 'Polygon';
        // Gebruik getBuildingTypeName() om de leesbare naam te tonen i.p.v. de UUID
        const functionName = getBuildingTypeName(selected.featureType);
        
        // Bouw de HTML op met basis informatie
        let html = `
            <div class="feature-info">
                <p><span class="label">ID:</span> <span class="value">${selected.id.substring(0, 8)}...</span></p>
                <p><span class="label">Soort:</span> <span class="value">${selected.geometry.type}</span></p>
                <p><span class="label">Functie:</span> <span class="value">${functionName}</span></p>
                <p><span class="label">${isPoly ? 'Hoogte' : 'Breedte'}:</span> <span class="value">${isPoly ? selected.height : selected.width}m</span></p>
        `;
        
        // Voor polygonen: toon extra info (naam, omschrijving) en bewerk knop
        if (isPoly) {
            const naam = selected.meta?.name || 'Geen naam';
            const omschrijving = selected.meta?.description || 'Geen omschrijving';
            html += `
                <hr class="info-divider" />
                <p><span class="label">Naam:</span> <span class="value">${naam}</span></p>
                <p><span class="label">Omschrijving:</span></p>
                <p class="description-text">${omschrijving}</p>
                <button id="btn-edit-polygon" class="btn btn-edit-details">Bewerk naam/omschrijving</button>
            `;
        }
        
        html += `</div>`;
        infoContent.innerHTML = html;
        
        // Event listener voor de bewerk knop toevoegen (alleen voor polygonen)
        if (isPoly) {
            const btnEditPolygon = document.getElementById('btn-edit-polygon');
            if (btnEditPolygon) {
                btnEditPolygon.onclick = () => {
                    // Haal de huidige gebouwtype naam op voor voorbeeldbeschrijvingen
                    const buildingTypeName = functionName;
                    const currentName = selected.meta?.name || '';
                    const currentDescription = selected.meta?.description || '';
                    
                    showEditModal(
                        selected,
                        buildingTypeName,
                        currentName,
                        currentDescription,
                        (newName, newDescription) => {
                            // Update de feature met nieuwe naam en beschrijving
                            if (editorInstance) {
                                const updatedMeta = { 
                                    ...selected.meta, 
                                    name: newName, 
                                    description: newDescription 
                                };
                                editorInstance.updateSelectedFeature({ meta: updatedMeta });
                                showToast('Naam en omschrijving bijgewerkt!', 'success');
                                
                                // Update de UI om de nieuwe waarden te tonen
                                const updatedFeature = featureStoreInstance.getFeature(selected.id);
                                if (updatedFeature) {
                                    updateUI(updatedFeature);
                                }
                            }
                        },
                        () => {
                            // Gebruiker heeft geannuleerd - niets doen
                        }
                    );
                };
            }
        }
        
        // Gebruik meta.typeId (UUID) voor de dropdown, niet featureType
        if (selectType && selected.meta && selected.meta.typeId) {
            selectType.value = selected.meta.typeId;
        }
        const val = isPoly ? (selected.height || 0) : (selected.width || 0);
        if (heightRange) heightRange.value = val;
        if (heightInput) heightInput.value = val;

    } else {
        infoContent.innerHTML = '<p class="muted">Klik op iets op de kaart om de details te zien.</p>';
    }
}

/**
 * Laat een kort berichtje (toast) zien linksonder.
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 500);
    }, 3000);
}

/**
 * Haalt de leesbare naam op van een gebouwtype UUID via de dropdown opties.
 * @param {string} typeId - De UUID van het gebouwtype
 * @returns {string} De naam van het gebouwtype of de UUID als fallback
 */
function getBuildingTypeName(typeId) {
    const selectType = document.getElementById('select-type');
    if (selectType) {
        // Loop door alle opties om de matching UUID te vinden
        const option = Array.from(selectType.options).find(o => o.value === typeId);
        if (option) {
            return option.textContent;  // Geeft bijv. "Vrijstaand huis"
        }
    }
    return typeId || 'Onbekend';  // Fallback: geef de UUID terug als er geen match is
}

// Start de hele handel
init();
