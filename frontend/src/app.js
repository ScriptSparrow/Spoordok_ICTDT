import { setupViewer, flyToSpoordok } from './cesium/viewer.js';
import { FeatureStore } from './editor/featureStore.js';
import { CesiumEditor } from './editor/cesiumEditor.js';
import { FeaturesApi } from './api/featuresApi.js';

/**
 * De main entry point van onze app.
 * Hier knopen we alles aan elkaar: de viewer, de store en de UI.
 */
async function init() {
    const featureStore = new FeatureStore();
    
    // We kijken of we lokaal moeten draaien (handig voor testen zonder backend)
    const useLocal = new URLSearchParams(window.location.search).has('local') || true;
    const api = new FeaturesApi('', useLocal);
    
    // Cesium viewer opstarten
    const viewer = await setupViewer('cesiumContainer');
    
    // Onze eigen editor initialiseren
    const editor = new CesiumEditor(viewer, featureStore, api, (selected) => {
        updateUI(selected);
    });

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

    // Type aanpassen als je de dropdown verandert
    if (selectType) {
        selectType.onchange = (e) => {
            const newType = e.target.value;
            editor.updateSelectedFeature({ featureType: newType });
            
            // Als we naar een weg switchen (of andersom), moeten we misschien de modus ook aanpassen?
            // Voor nu laten we het bij het updaten van de geselecteerde feature.
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

    // Alles ophalen uit de backend (of lokale store) bij het opstarten
    try {
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
        infoContent.innerHTML = `
            <div class="feature-info">
                <p><span class="label">ID:</span> <span class="value">${selected.id.substring(0, 8)}...</span></p>
                <p><span class="label">Soort:</span> <span class="value">${selected.geometry.type}</span></p>
                <p><span class="label">Functie:</span> <span class="value">${selected.featureType}</span></p>
                <p><span class="label">${isPoly ? 'Hoogte' : 'Breedte'}:</span> <span class="value">${isPoly ? selected.height : selected.width}m</span></p>
            </div>
        `;
        
        if (selectType) selectType.value = selected.featureType;
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

// Start de hele handel
init();
