import { Ion, Viewer, Terrain, Cartesian3, Math as CesiumMath, createOsmBuildingsAsync, ScreenSpaceEventType, Color } from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';

// Stel het Cesium basispad in voor statische assets (zoals icoontjes en webworkers)
window.CESIUM_BASE_URL = '/Cesium';

// Stel de Ion‑token in voor de kaartlagen en 3D data
Ion.defaultAccessToken = (import.meta?.env && import.meta.env.VITE_CESIUM_ION_TOKEN) || (typeof process !== 'undefined' ? process.env.CESIUM_ACCESS_TOKEN : undefined) || Ion.defaultAccessToken;

// Het middelpunt van Spoordok (Leeuwarden) waar we de camera op mikken
export const SPOORDOK_CENTER = { lon: 5.7805, lat:  53.1913, height: 600 };

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

  // Zorg dat de rechtermuisknop niet de contextmenu van de browser opent
  viewer.canvas.addEventListener('contextmenu', (e) => e.preventDefault());

  // Voeg een statische polygon toe voor het Spoordok gebied
  viewer.entities.add({
    name: "Spoordok",
    polygon: {
      hierarchy: Cartesian3.fromDegreesArray([
        5.787759928698073, 53.197831145908,
        5.789123554275904, 53.19763995957844,
        5.788934967759822, 53.19602353198474,
        5.776937964005922, 53.194528716741345,
        5.774587885853288, 53.196901277127026,
        5.774703939093954, 53.1976225789762,
        5.786410809746187, 53.19704032421097,
      ]),
      material: Color.GRAY.withAlpha(0.9),
      zIndex: -1,
    },
  });

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
