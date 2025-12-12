import { CallbackProperty, Cartographic, Cartesian2, Cartesian3, Color, Entity, HeightReference, LabelStyle, PolygonGraphics, PolylineGraphics, ScreenSpaceEventHandler, ScreenSpaceEventType, Viewer, defined, Math as CesiumMath, Matrix4, Transforms } from 'cesium';
import { KENGETALLEN } from './kengetallen.js';

// Kleurtoewijzing per functie/type - gegenereerd uit KENGETALLEN
const TYPE_COLORS = Object.fromEntries(
  Object.entries(KENGETALLEN).map(([code, k]) => [
    code,
    Color.fromCssColorString(k.color).withAlpha(0.6)
  ])
);

export class PolygonEditor {
  /**
   * @param {Viewer} viewer
   * @param {{onPolygonCreated?:Function,onPolygonUpdated?:Function,onPolygonDeleted?:Function,onSelectionChanged?:Function}} callbacks
   */
  constructor(viewer, callbacks = {}) {
    this.viewer = viewer;
    this.callbacks = callbacks;

    this.mode = 'idle'; // 'draw' | 'edit' | 'idle'
    this.currentType = 'B'; // Rijtjeswoning als standaard
    this.currentHeight = 10;

    this.handler = new ScreenSpaceEventHandler(this.viewer.canvas);

    // Gegevens
    this.polygons = []; // { id, entity, positions: Cartesian3[], type, height, backendId, areaM2, volumeM3, lonLatHeight }
    this.selected = null;

    // Ongedaan maken / Opnieuw (undo/redo)
    this.undoStack = [];
    this.redoStack = [];

    // Tijdelijke tekenstatus
    this.activePositions = [];
    this.dynamicEntity = null;

    // Bewerk-handles / sleepstatus
    this._handles = [];
    this._dragging = null; // { type: 'vertex'|'whole', index?, vooraf, basisPosities?, startPos? }

    this._bindInputEvents();
  }

  setMode(mode) {
    this.mode = mode;
    if (mode === 'draw') {
      this._startDrawing();
    } else if (mode === 'edit') {
      this._stopDrawing();
    } else {
      this._stopDrawing();
    }
  }

  setCurrentType(type) {
    this.currentType = type;
    if (this.selected) {
      this.selected.type = type;
      this._refreshVisual(this.selected);
      this._computeMetrics(this.selected);
      this._emitUpdate(this.selected);
      this._notifyStyleChanged();
    }
  }

  setCurrentHeight(h) {
    this.currentHeight = Number(h) || 0;
    if (this.selected) {
      this.selected.height = this.currentHeight;
      this._refreshVisual(this.selected);
      this._computeMetrics(this.selected);
      this._emitUpdate(this.selected);
      this._notifyStyleChanged();
    }
  }

  cancel() {
    this._stopDrawing(true);
  }

  deleteSelected() {
    if (!this.selected) return;
    const poly = this.selected;
    this.viewer.entities.remove(poly.entity);
    this.polygons = this.polygons.filter(p => p !== poly);
    this._pushUndo({ action: 'delete', poly });
    this._select(null);
    this.callbacks.onPolygonDeleted && this.callbacks.onPolygonDeleted(poly);
  }

  undo() {
    const item = this.undoStack.pop();
    if (!item) return;
    this._applyInverse(item);
    this.redoStack.push(item);
  }
  redo() {
    const item = this.redoStack.pop();
    if (!item) return;
    this._apply(item);
    this.undoStack.push(item);
  }

  _pushUndo(change) {
    this.undoStack.push(structuredClone(change));
    this.redoStack.length = 0;
  }

  onCurrentStyleChanged(_) {}
  _notifyStyleChanged() {
    this.onCurrentStyleChanged({ type: this.currentType, height: this.currentHeight });
  }

  _bindInputEvents() {
    // Linkermuisklik
    this.handler.setInputAction((movement) => {
      const pos = this._pickPosition(movement.position);
      if (!pos && this.mode === 'draw') return;

      if (this.mode === 'draw') {
        // Sluit polygon bij klik dicht bij het eerste punt (en minimaal 3 punten)
        if (this.activePositions.length >= 3 && pos) {
          const first = this.activePositions[0];
          if (Cartesian3.distance(first, pos) < 3) {
            this._finishDrawing();
            return;
          }
        }
        if (pos) {
          this.activePositions.push(pos);
          this._updateDynamicEntity();
        }
      } else if (this.mode === 'edit') {
        // Begin met slepen indien een handle is geklikt
        const picked = this.viewer.scene.pick(movement.position);
        if (picked && picked.id && picked.id._isHandle) {
          this._beginDragHandle(picked.id._handleIndex);
        } else if (picked && picked.id && this.polygons.some(p => p.entity === picked.id)) {
          const poly = this.polygons.find(p => p.entity === picked.id);
          this._select(poly);
          this._beginDragWhole();
        } else {
          // selection change
          this._trySelect(movement.position);
        }
      } else {
        this._trySelect(movement.position);
      }
    }, ScreenSpaceEventType.LEFT_CLICK);

    // Linker muisknop ingedrukt om slepen te starten (alternatief in sommige browsers)
    this.handler.setInputAction((movement) => {
      if (this.mode !== 'edit') return;
      const picked = this.viewer.scene.pick(movement.position);
      if (picked && picked.id && picked.id._isHandle) {
        this._beginDragHandle(picked.id._handleIndex);
      } else if (picked && picked.id && this.polygons.some(p => p.entity === picked.id)) {
        const poly = this.polygons.find(p => p.entity === picked.id);
        this._select(poly);
        this._beginDragWhole();
      }
    }, ScreenSpaceEventType.LEFT_DOWN);

    this.handler.setInputAction(() => {
      if (this._dragging) this._endDrag();
    }, ScreenSpaceEventType.LEFT_UP);

    // Rechtermuisklik om tekenen te voltooien
    this.handler.setInputAction(() => {
      if (this.mode === 'draw' && this.activePositions.length >= 3) {
        this._finishDrawing();
      }
    }, ScreenSpaceEventType.RIGHT_CLICK);

    // Mouse move for dynamic preview or dragging
    this.handler.setInputAction((movement) => {
      if (this.mode === 'draw') {
        const pos = this._pickPosition(movement.endPosition);
        this._updateDynamicEntity(pos || null);
        return;
      }
      if (this.mode === 'edit' && this._dragging) {
        const pos = this._pickPosition(movement.endPosition);
        if (!pos) return;
        this._continueDrag(pos);
      }
    }, ScreenSpaceEventType.MOUSE_MOVE);
  }

  _startDrawing() {
    this.activePositions = [];
    this._updateDynamicEntity(null);
  }

  _stopDrawing(cancel = false) {
    if (this.dynamicEntity) {
      this.viewer.entities.remove(this.dynamicEntity);
      this.dynamicEntity = null;
    }
    if (cancel) this.activePositions = [];
  }

  _finishDrawing() {
    const positions = [...this.activePositions];
    this._stopDrawing();
    if (positions.length < 3) return;

    const entity = this.viewer.entities.add({
      polygon: {
        hierarchy: positions,
        material: (TYPE_COLORS[this.currentType] || Color.ORANGE).clone(),
        heightReference: HeightReference.CLAMP_TO_GROUND,
        perPositionHeight: false,
        outline: true,
        outlineColor: Color.BLACK.withAlpha(0.4),
      },
      polyline: new PolylineGraphics({
        positions: [...positions, positions[0]],
        width: 2,
        material: Color.BLACK.withAlpha(0.4),
        clampToGround: true,
      })
    });

    const poly = {
      id: crypto.randomUUID(),
      entity,
      positions,
      type: this.currentType,
      height: this.currentHeight,
      backendId: null,
      areaM2: 0,
      volumeM3: 0,
      lonLatHeight: [],
    };
    this._computeMetrics(poly);
    this.polygons.push(poly);
    this._pushUndo({ action: 'create', poly: this._snapshot(poly) });
    this._select(poly);
    this.callbacks.onPolygonCreated && this.callbacks.onPolygonCreated(poly);
  }

  _snapshot(poly) {
    return {
      id: poly.id,
      type: poly.type,
      height: poly.height,
      positions: poly.positions.map(p => Cartesian3.clone(p)),
      backendId: poly.backendId,
      lonLatHeight: poly.lonLatHeight?.map(a => [...a]),
      areaM2: poly.areaM2,
      volumeM3: poly.volumeM3,
    };
  }

  _apply(change) {
    if (change.action === 'create') {
      // Herstel: polygon opnieuw aanmaken
      const positions = change.poly.positions.map(p => Cartesian3.clone(p));
      const prevType = change.poly.type;
      const prevHeight = change.poly.height;
      this.currentType = prevType;
      this.currentHeight = prevHeight;
      this.activePositions = positions;
      this._finishDrawing();
    } else if (change.action === 'delete') {
      // Opnieuw verwijderen
      const poly = this.polygons.find(p => p.id === change.poly.id);
      if (poly) {
        this.viewer.entities.remove(poly.entity);
        this.polygons = this.polygons.filter(p => p !== poly);
        if (this.selected === poly) this._select(null);
      }
    } else if (change.action === 'update') {
      const poly = this.polygons.find(p => p.id === change.after.id);
      if (poly) this._restoreState(poly, change.after);
    }
  }

  _applyInverse(change) {
    if (change.action === 'create') {
      // Remove the created poly
      const poly = this.polygons.find(p => p.id === change.poly.id);
      if (poly) {
        this.viewer.entities.remove(poly.entity);
        this.polygons = this.polygons.filter(p => p !== poly);
        if (this.selected === poly) this._select(null);
      }
    } else if (change.action === 'delete') {
      // Recreate the deleted poly
      const c = change.poly;
      const entity = this.viewer.entities.add({
        polygon: {
          hierarchy: c.positions,
          material: (TYPE_COLORS[c.type] || Color.ORANGE).clone(),
          heightReference: HeightReference.CLAMP_TO_GROUND,
          perPositionHeight: false,
          outline: true,
          outlineColor: Color.BLACK.withAlpha(0.4),
        },
        polyline: new PolylineGraphics({ positions: [...c.positions, c.positions[0]], width: 2, material: Color.BLACK.withAlpha(0.4), clampToGround: true })
      });
      const poly = { ...c, entity };
      this.polygons.push(poly);
      this._select(poly);
    } else if (change.action === 'update') {
      const poly = this.polygons.find(p => p.id === change.before.id);
      if (poly) this._restoreState(poly, change.before);
    }
  }

  _restoreState(poly, snapshot) {
    poly.type = snapshot.type;
    poly.height = snapshot.height;
    poly.positions = snapshot.positions.map(p => Cartesian3.clone(p));
    poly.backendId = snapshot.backendId;
    this._refreshVisual(poly);
    this._computeMetrics(poly);
    this._select(poly);
  }

  _refreshVisual(poly) {
    const color = (TYPE_COLORS[poly.type] || Color.ORANGE).clone();
    if (poly.entity.polygon) {
      poly.entity.polygon.material = color;
      poly.entity.polygon.hierarchy = poly.positions;
    }
    if (poly.entity.polyline) {
      poly.entity.polyline.positions = [...poly.positions, poly.positions[0]];
    }
  }

  _trySelect(windowPosition) {
    const picked = this.viewer.scene.pick(windowPosition);
    if (picked && picked.id && this.polygons.some(p => p.entity === picked.id)) {
      const poly = this.polygons.find(p => p.entity === picked.id);
      this._select(poly);
    } else {
      this._select(null);
    }
  }

  _select(poly) {
    this.selected = poly;
    this._clearHandles();
    if (poly) {
      this.currentType = poly.type;
      this.currentHeight = poly.height;
      this._notifyStyleChanged();
      if (this.mode === 'edit') this._refreshHandles(poly);
    }
    this.callbacks.onSelectionChanged && this.callbacks.onSelectionChanged(poly);
  }

  setCurrentHeightForSelected(h) {
    if (!this.selected) return;
    const before = this._snapshot(this.selected);
    this.selected.height = Number(h) || 0;
    this._computeMetrics(this.selected);
    this._refreshVisual(this.selected);
    this._pushUndo({ action: 'update', before, after: this._snapshot(this.selected) });
    this._emitUpdate(this.selected);
  }

  setCurrentTypeForSelected(t) {
    if (!this.selected) return;
    const before = this._snapshot(this.selected);
    this.selected.type = t;
    this._refreshVisual(this.selected);
    this._pushUndo({ action: 'update', before, after: this._snapshot(this.selected) });
    this._emitUpdate(this.selected);
  }

  setCurrentHeight(h) { this.setCurrentHeightForSelected(h); }
  setCurrentType(t) { this.setCurrentTypeForSelected(t); }

  deletePolygonById(id) {
    const poly = this.polygons.find(p => p.id === id);
    if (!poly) return;
    this.viewer.entities.remove(poly.entity);
    this.polygons = this.polygons.filter(p => p !== poly);
    this._pushUndo({ action: 'delete', poly: this._snapshot(poly) });
    if (this.selected === poly) this._select(null);
    this.callbacks.onPolygonDeleted && this.callbacks.onPolygonDeleted(poly);
  }

  _emitUpdate(poly) {
    this.callbacks.onPolygonUpdated && this.callbacks.onPolygonUpdated(poly);
  }

  _pickPosition(windowPosition) {
    const scene = this.viewer.scene;
    const earthPos = scene.pickPosition(windowPosition) || this._pickOnGlobe(windowPosition);
    return earthPos || null;
  }

  _pickOnGlobe(windowPosition) {
    const ray = this.viewer.camera.getPickRay(windowPosition);
    if (!ray) return null;
    const pos = this.viewer.scene.globe.pick(ray, this.viewer.scene);
    return pos || null;
  }

  _updateDynamicEntity(cursorPos = null) {
    const positions = [...this.activePositions];
    if (cursorPos) positions.push(cursorPos);

    if (!this.dynamicEntity) {
      this.dynamicEntity = this.viewer.entities.add({
        polyline: new PolylineGraphics({ positions, width: 2, material: Color.WHITE.withAlpha(0.6), clampToGround: true })
      });
    } else {
      this.dynamicEntity.polyline.positions = positions;
    }
  }

  _refreshHandles(poly) {
    this._clearHandles();
    if (!poly) return;
    poly.positions.forEach((p, i) => {
      const handle = this.viewer.entities.add({
        position: p,
        point: { pixelSize: 10, color: Color.WHITE, outlineColor: Color.BLACK, outlineWidth: 2, heightReference: HeightReference.CLAMP_TO_GROUND },
      });
      handle._isHandle = true;
      handle._handleIndex = i;
      this._handles.push(handle);
    });
  }

  _clearHandles() {
    if (this._handles?.length) {
      this._handles.forEach(h => this.viewer.entities.remove(h));
      this._handles.length = 0;
    }
  }

  _beginDragHandle(index) {
    if (!this.selected) return;
    this._dragging = {
      type: 'vertex',
      index,
      before: this._snapshot(this.selected),
    };
  }

  _beginDragWhole() {
    if (!this.selected) return;
    this._dragging = {
      type: 'whole',
      before: this._snapshot(this.selected),
      basePositions: this.selected.positions.map(p => Cartesian3.clone(p)),
      startPos: null,
    };
  }

  _continueDrag(pos) {
    if (!this._dragging || !this.selected) return;
    if (this._dragging.type === 'vertex') {
      this.selected.positions[this._dragging.index] = pos;
    } else if (this._dragging.type === 'whole') {
      if (!this._dragging.startPos) this._dragging.startPos = pos;
      const delta = Cartesian3.subtract(pos, this._dragging.startPos, new Cartesian3());
      this.selected.positions = this._dragging.basePositions.map(p0 => Cartesian3.add(p0, delta, new Cartesian3()));
    }
    this._refreshVisual(this.selected);
    this._computeMetrics(this.selected);
    this._refreshHandles(this.selected);
  }

  _endDrag() {
    if (!this._dragging || !this.selected) { this._dragging = null; return; }
    const change = { action: 'update', before: this._dragging.before, after: this._snapshot(this.selected) };
    this._pushUndo(change);
    this._emitUpdate(this.selected);
    this._dragging = null;
  }

  _computeMetrics(poly) {
    // Convert positions to local ENU plane at centroid, compute area via shoelace
    const cartographics = poly.positions.map(p => Cartographic.fromCartesian(p));
    const centroid = averageCartographic(cartographics);
    const origin = Cartographic.toCartesian(centroid);
    const enu = Transforms.eastNorthUpToFixedFrame(origin);

    const xy = poly.positions.map(p => {
      const v = Cartesian3.clone(p);
      const local = Matrix4.inverseTransformation(enu, new Matrix4());
      const out = new Cartesian3();
      Cartesian3.multiplyByMatrix4(v, local, out);
      return new Cartesian2(out.x, out.y);
    });

    const area = Math.abs(shoelaceArea(xy)); // m^2
    const height = Number(poly.height) || 0;
    const volume = area * height; // m^3 (prism approx)

    poly.areaM2 = round(area, 2);
    poly.volumeM3 = round(volume, 2);

    // Also store lon/lat/height coordinates for backend
    poly.lonLatHeight = cartographics.map(c => [CesiumMath.toDegrees(c.longitude), CesiumMath.toDegrees(c.latitude), height]);
  }
}

function averageCartographic(list) {
  let sx = 0, sy = 0, sz = 0;
  for (const c of list) {
    sx += c.longitude; sy += c.latitude; sz += c.height || 0;
  }
  const n = list.length || 1;
  return new Cartographic(sx / n, sy / n, sz / n);
}

function shoelaceArea(points) {
  let sum = 0;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    sum += (points[j].x + points[i].x) * (points[j].y - points[i].y);
  }
  return sum / 2;
}

function round(v, d) { const p = Math.pow(10, d); return Math.round(v * p) / p; }
