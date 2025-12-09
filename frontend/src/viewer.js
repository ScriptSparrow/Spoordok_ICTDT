import { Ion, Viewer, Terrain, Cartesian3, Math as CesiumMath, createOsmBuildingsAsync, ScreenSpaceEventType } from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';

// Stel het Cesium basispad in voor statische assets
window.CESIUM_BASE_URL = '/Cesium';

// Stel de Ion‑token in indien aanwezig via omgevingsvariabelen
Ion.defaultAccessToken = (import.meta?.env && import.meta.env.VITE_CESIUM_ION_TOKEN) || (typeof process !== 'undefined' ? process.env.CESIUM_ACCESS_TOKEN : undefined) || Ion.defaultAccessToken;

// Benaderd middelpunt van Spoordok (Leeuwarden) [lon, lat]
export const SPOORDOK_CENTER = { lon: 5.7920, lat: 53.1969, height: 400 };

export function setupViewer(containerId) {
  const viewer = new Viewer(containerId, {
    terrain: Terrain.fromWorldTerrain(),
    animation: false,
    timeline: false,
    geocoder: false,
    baseLayerPicker: true,
    sceneModePicker: true,
    navigationHelpButton: true,
    homeButton: true,
    shouldAnimate: false,
    infoBox: false,
    selectionIndicator: false,
  });

  // Zet standaard dubbelklik-actie (inzoomen/reset camera) uit om onbedoelde camera-jumps te voorkomen
  viewer.cesiumWidget.screenSpaceEventHandler.removeInputAction(ScreenSpaceEventType.LEFT_DOUBLE_CLICK);

  // Voeg een wereldwijde gebouwenlaag toe voor context
  createOsmBuildingsAsync().then(ts => viewer.scene.primitives.add(ts)).catch(() => {});

  // Maak de globe iets donkerder zodat panelen beter opvallen
  viewer.scene.globe.baseColor = viewer.scene.globe.baseColor.withAlpha?.(0.98) || viewer.scene.globe.baseColor;

  return viewer;
}

export async function flyToSpoordok(viewer) {
  await viewer.camera.flyTo({
    destination: Cartesian3.fromDegrees(SPOORDOK_CENTER.lon, SPOORDOK_CENTER.lat, SPOORDOK_CENTER.height),
    orientation: {
      heading: CesiumMath.toRadians(15),
      pitch: CesiumMath.toRadians(-35),
    },
    duration: 1.8,
  });
}
