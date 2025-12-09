import { setupViewer, flyToSpoordok, SPOORDOK_CENTER } from './viewer.js';
import { PolygonEditor } from './editor.js';
import { ApiClient } from './api.js';
import { InfoPanel, Toasts, bindControls } from './ui.js';

// Ingangspunt van de applicatie
(async function main() {
  const viewer = setupViewer('cesiumContainer');
  await flyToSpoordok(viewer);

  const api = new ApiClient({ baseUrl: import.meta.env?.VITE_API_BASE || process.env.API_BASE_URL || '' });
  const toasts = new Toasts(document.getElementById('toast-container'));
  const infoPanel = new InfoPanel(document.getElementById('info-content'), document.getElementById('simulation-result'));

  // Editorstatus
  const editor = new PolygonEditor(viewer, {
    onPolygonCreated: async (poly) => {
      try {
        const payload = toBackendPayload(poly);
        const saved = await api.createPolygon(payload);
        poly.backendId = saved.id;
        toasts.success('Polygon opgeslagen');
      } catch (e) {
        console.error(e);
        toasts.error('Opslaan mislukt');
      }
    },
    onPolygonUpdated: async (poly) => {
      if (!poly.backendId) return; // nothing yet
      try {
        const payload = toBackendPayload(poly);
        await api.updatePolygon(poly.backendId, payload);
        toasts.success('Polygon bijgewerkt');
      } catch (e) {
        console.error(e);
        toasts.error('Bijwerken mislukt');
      }
    },
    onPolygonDeleted: async (poly) => {
      if (!poly.backendId) return;
      try {
        await api.deletePolygon(poly.backendId);
        toasts.success('Polygon verwijderd');
      } catch (e) {
        console.error(e);
        toasts.error('Verwijderen faalde');
      }
    },
    onSelectionChanged: async (poly) => {
      if (!poly) { infoPanel.clear(); return; }
      infoPanel.showPolygon(poly);
      if (poly.scenarioId) {
        try {
          const data = await api.getScenarioScores(poly.scenarioId);
          // Probeer gangbare vormen: ofwel directe velden of geneste objecten
          const scores = data?.scores || data?.kengetallen || data;
          const llm = data?.llm || data?.ai || null;
          infoPanel.showPolygon(poly, { scores, llm });
        } catch (e) {
          console.warn('Scores ophalen mislukt', e);
        }
      }
    }
  });

  // Koppel UI-bediening
  const ui = bindControls({
    onDraw: () => editor.setMode('draw'),
    onEdit: () => editor.setMode('edit'),
    onDelete: () => editor.deleteSelected(),
    onUndo: () => editor.undo(),
    onRedo: () => editor.redo(),
    onTypeChange: (t) => editor.setCurrentType(t),
    onHeightChange: (h) => editor.setCurrentHeight(h),
    onResetCamera: () => flyToSpoordok(viewer),
    onSimulate: async () => {
      try {
        toasts.info('Simulatie gestart...');
        const resp = await api.runAgents({ center: SPOORDOK_CENTER });
        infoPanel.showSimulation(resp);
        toasts.success('Simulatie gereed');
      } catch (e) {
        console.error(e);
        toasts.error('Simulatie mislukt');
      }
    }
  });

  // Synchroniseer linkerpanel-invoer met de huidige selectie
  editor.onCurrentStyleChanged = ({ type, height }) => ui.syncStyle(type, height);

  // Sneltoetsen
  window.addEventListener('keydown', (ev) => {
    if (ev.key === 'Escape') editor.cancel();
    if (ev.key === 'Delete') editor.deleteSelected();
    if ((ev.ctrlKey || ev.metaKey) && ev.key.toLowerCase() === 'z') editor.undo();
    if ((ev.ctrlKey || ev.metaKey) && ev.key.toLowerCase() === 'y') editor.redo();
  });
})();

function toBackendPayload(poly) {
  return {
    geometry: { coordinates: poly.lonLatHeight, height: poly.height || 0 },
    type: poly.type,
    volume_m3: poly.volumeM3,
    area_m2: poly.areaM2,
    scenarioId: poly.scenarioId || null
  };
}
