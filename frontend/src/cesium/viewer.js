import { Ion, Viewer, Terrain, Cartesian3, Math as CesiumMath, createOsmBuildingsAsync, ScreenSpaceEventType } from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';

// Stel het Cesium basispad in voor statische assets (zoals icoontjes en webworkers)
window.CESIUM_BASE_URL = '/Cesium';

// Stel de Ion‑token in voor de kaartlagen en 3D data
Ion.defaultAccessToken = (import.meta?.env && import.meta.env.VITE_CESIUM_ION_TOKEN) || (typeof process !== 'undefined' ? process.env.CESIUM_ACCESS_TOKEN : undefined) || Ion.defaultAccessToken;

// Het middelpunt van Spoordok (Leeuwarden) waar we de camera op mikken
export const SPOORDOK_CENTER = { lon: 5.7920, lat: 53.1969, height: 400 };

/**
 * Maakt de Cesium Viewer aan met de juiste instellingen.
 */
export function setupViewer(containerId) {
  const viewer = new Viewer(containerId, {
    terrain: Terrain.fromWorldTerrain(), // Wereldwijd terrein (hoogteverschillen)
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

  // Haal de standaard dubbelklik-actie weg, anders springt de camera steeds weg
  viewer.cesiumWidget.screenSpaceEventHandler.removeInputAction(ScreenSpaceEventType.LEFT_DOUBLE_CLICK);

  // Gebouwen uit OpenStreetMap inladen voor de context, comment deze regel om ze niet te zien
  //createOsmBuildingsAsync().then(ts => viewer.scene.primitives.add(ts)).catch(() => {});

  // Maak de aardbol wat donkerder voor het contrast met de menu's
  viewer.scene.globe.baseColor = viewer.scene.globe.baseColor.withAlpha?.(0.98) || viewer.scene.globe.baseColor;

  // Zorg dat we ook doorzichtige dingen kunnen aanklikken
  viewer.scene.pickTranslucentDepth = true;
  
  // Zorg dat de diepte klopt met het terrein
  viewer.scene.globe.depthTestAgainstTerrain = true;

  return viewer;
}

/**
 * Vliegt de camera naar Spoordok
 */
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
