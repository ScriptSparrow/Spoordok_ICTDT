import { setupViewer, flyToSpoordok } from './cesium/viewer.js';
import { FeatureStore } from './editor/featureStore.js';
import { CesiumEditor } from './editor/cesiumEditor.js';
import { FeaturesApi } from './api/featuresApi.js';
import { FeaturesApi as BuildingTypesApi } from './api/buildTypeApi.js';
import { showEditModal } from './ui/descriptionModal.js';

// Module-level variabelen voor toegang vanuit updateUI
let currentEditor = null;
let currentFeatureStore = null;

/**
 * De main entry point van onze app.
 * Hier knopen we alles aan elkaar: de viewer, de store en de UI.
 */
async function init() {
    const featureStore = new FeatureStore();
    currentFeatureStore = featureStore;  // Module-level toegang
    
    // We kijken of we lokaal moeten draaien (handig voor testen zonder backend)
    const useLocal = false;
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
    const api = new FeaturesApi(apiBaseUrl, useLocal);
    
    // Cesium viewer opstarten
    const viewer = await setupViewer('cesiumContainer');
    
    // Onze eigen editor initialiseren
    const editor = new CesiumEditor(viewer, featureStore, api, (selected) => {
        updateUI(selected);
    });
    currentEditor = editor;  // Module-level toegang

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
    
    if (btnDelete) btnDelete.onclick = () => editor.deleteSelected();
    
    if (btnUndo) btnUndo.onclick = () => editor.undo();
    if (btnRedo) btnRedo.onclick = () => editor.redo();
    
    if (btnReset) btnReset.onclick = () => flyToSpoordok(viewer);

    // Type aanpassen als je de dropdown verandert
    if (selectType) {
        selectType.onchange = (e) => {
            const newType = e.target.value;  // UUID van de database
            const selected = featureStore.getFeature(editor.selectedId);
            if (selected) {
                // Haal de kleur uit het data-color attribuut van de geselecteerde optie
                const selectedOption = e.target.selectedOptions[0];
                const newColor = selectedOption?.dataset?.color || '#ffffff';
                
                // Update zowel featureType (voor UI kleuren) als meta.typeId en color (voor API/database)
                editor.updateSelectedFeature({ 
                    featureType: newType,
                    meta: { 
                        ...selected.meta,  // Behoud bestaande meta properties (naam, beschrijving)
                        typeId: newType,   // Update de typeId met de nieuwe UUID
                        color: newColor    // Update de kleur
                    }
                });
            }
            console.log(`Type veranderd naar: ${newType}`);
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
    const buildingTypesApi = new BuildingTypesApi(apiBaseUrl);
    try {
        const buildingTypes = await buildingTypesApi.getList();
        const selectType = document.getElementById('select-type');
        
        if (selectType) {
            // Leeg de placeholder optie
            selectType.innerHTML = '';
            
            // Vul met gebouwtypes uit de database (filter "Wegen" eruit)
            buildingTypes
                .filter(type => type.labelName !== 'Wegen')
                .forEach(type => {
                    const option = document.createElement('option');
                    option.value = type.buildingTypeId;  // UUID als value
                    option.textContent = type.labelName;  // Naam als tekst
                    option.dataset.color = type.color;   // Kleur opslaan voor later gebruik
                    selectType.appendChild(option);
                });
            
            console.log(`Dropdown gevuld met ${buildingTypes.length - 1} gebouwtypes`);
        }
    } catch (e) {
        console.warn('Kon gebouwtypes niet laden:', e);
        showToast('Gebouwtypes konden niet geladen worden', 'error');
    }

    // Alles ophalen uit de backend (of lokale store) bij het opstarten
    try {
        // Gebouwtypes ophalen voor de dropdown en mapping
        const buildingTypes = await api.getBuildingTypes();
        editor.setBuildingTypes(buildingTypes);
        
        const features = await api.getAll();
        features.forEach(f => {
            featureStore.addFeature(f);
            editor.syncEntity(f);
        });
        console.log(`Lekker bezig, ${features.length} features ingeladen!`);
    } catch (e) {
        console.warn('Backend niet gevonden, we gaan lokaal verder');
        showToast('Server niet bereikbaar - we slaan het lokaal voor je op', 'error');
    }

    // Camera vliegen naar Spoordok
    flyToSpoordok(viewer);
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
    return typeId;  // Fallback: geef de UUID terug als er geen match is
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
        
        // Basis info HTML
        let html = `
            <div class="feature-info">
                <p><span class="label">ID:</span> <span class="value">${selected.id.substring(0, 8)}...</span></p>
                <p><span class="label">Soort:</span> <span class="value">${selected.geometry.type}</span></p>
                <p><span class="label">Functie:</span> <span class="value">${getBuildingTypeName(selected.featureType)}</span></p>
                <p><span class="label">${isPoly ? 'Hoogte' : 'Breedte'}:</span> <span class="value">${isPoly ? selected.height : selected.width}m</span></p>
        `;
<<<<<<< HEAD
=======
        const meta = selected.meta || {};
        
        if (isPoly) {
            infoContent.innerHTML = `
                <div class="feature-info">
                    <h3>${meta.name || 'Naamloos Gebouw'}</h3>
                    <p><em>${meta.description || 'Geen omschrijving'}</em></p>
                    <hr>
                    <p><span class="label">ID:</span> <span class="value">${selected.id.substring(0, 8)}...</span></p>
                    <p><span class="label">Type:</span> <span class="value">${meta.typeLabel || 'Onbekend'}</span></p>
                    <p><span class="label">Hoogte:</span> <span class="value">${selected.height}m</span></p>
                    <hr>
                    <p><span class="label">Kosten:</span> <span class="value">€${meta.costPerUnit || 0} per ${meta.unit || 'eenheid'}</span></p>
                    <p><span class="label">Bewoners:</span> <span class="value">${meta.residentsPerUnit || 0}</span></p>
                    <p><span class="label">Punten:</span> <span class="value">${meta.points || 0}</span></p>
                    <p><span class="label">Bewoonbaar:</span> <span class="value">${meta.inhabitable ? 'Ja' : 'Nee'}</span></p>
                </div>
            `;
        } else {
            infoContent.innerHTML = `
                <div class="feature-info">
                    <p><span class="label">ID:</span> <span class="value">${selected.id.substring(0, 8)}...</span></p>
                    <p><span class="label">Soort:</span> <span class="value">${selected.geometry.type}</span></p>
                    <p><span class="label">Functie:</span> <span class="value">${selected.featureType}</span></p>
                    <p><span class="label">Breedte:</span> <span class="value">${selected.width}m</span></p>
                </div>
            `;
        }
>>>>>>> refs/rewritten/merged-main
=======
>>>>>>> 332b965 (merged main)
        
        // Voor polygonen: toon naam, omschrijving en bewerkknop
        if (isPoly) {
            const naam = selected.meta?.name || 'Geen naam';
            const omschrijving = selected.meta?.description || 'Geen omschrijving';
            
            html += `
                <hr class="info-divider" />
                <p><span class="label">Naam:</span> <span class="value">${naam}</span></p>
                <p><span class="label">Omschrijving:</span></p>
                <p class="description-text">${omschrijving}</p>
                <button id="btn-edit-polygon" class="btn btn-edit">Bewerken</button>
            `;
        }
        
        html += `</div>`;
        infoContent.innerHTML = html;
        
        // Event listener voor de bewerkknop
        if (isPoly) {
            const btnEdit = document.getElementById('btn-edit-polygon');
            if (btnEdit) {
                btnEdit.onclick = () => openEditModal(selected);
            }
        }
        
        if (selectType) selectType.value = selected.featureType;
        const val = isPoly ? (selected.height || 0) : (selected.width || 0);
        if (heightRange) heightRange.value = val;
        if (heightInput) heightInput.value = val;

    } else {
        infoContent.innerHTML = '<p class="muted">Klik op iets op de kaart om de details te zien.</p>';
    }
}

/**
 * Opent de modal om een bestaande polygon te bewerken.
 * @param {Object} feature - De te bewerken feature
 */
function openEditModal(feature) {
    const buildingTypeName = getBuildingTypeName(feature.featureType);
    const currentName = feature.meta?.name || '';
    const currentDescription = feature.meta?.description || '';
    
    showEditModal(
        feature,
        buildingTypeName,
        currentName,
        currentDescription,
        (newName, newDescription) => {
            // Gebruiker heeft opgeslagen - update de feature
            if (currentEditor && currentFeatureStore) {
                currentEditor.updateSelectedFeature({
                    meta: {
                        ...feature.meta,
                        name: newName,
                        description: newDescription
                    }
                });
                showToast('Polygon bijgewerkt!', 'success');
                
                // Update de UI met de nieuwe gegevens
                const updatedFeature = currentFeatureStore.getFeature(feature.id);
                if (updatedFeature) {
                    updateUI(updatedFeature);
                }
            }
        },
        () => {
            // Gebruiker heeft geannuleerd
            showToast('Bewerken geannuleerd', 'info');
        }
    );
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

// Start de hele handel
init();
