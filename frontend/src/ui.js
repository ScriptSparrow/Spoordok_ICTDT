import { KENGETALLEN, calculateCosts } from './kengetallen.js';

export class Toasts {
  constructor(container) {
    this.container = container;
  }
  _push(text, cls = '') {
    const div = document.createElement('div');
    div.className = `toast ${cls}`.trim();
    div.textContent = text;
    this.container.appendChild(div);
    setTimeout(() => div.remove(), 3500);
  }
  info(t) { this._push(t); }
  success(t) { this._push(t, 'success'); }
  error(t) { this._push(t, 'error'); }
}

export class InfoPanel {
  constructor(contentEl, simEl) {
    this.contentEl = contentEl;
    this.simEl = simEl;
  }

  clear() {
    this.contentEl.innerHTML = '<p>Selecteer een polygon om details te zien.</p>';
  }

  showPolygon(poly, extra = {}) {
    const { costs = '-', opbrengst = '-', scores = null, llm = null, warnings = [] } = extra;
    const list = [];
    list.push(row('Functie', labelForType(poly.type)));
    list.push(row('Hoogte', `${poly.height ?? 0} m`));
    list.push(row('Oppervlakte', `${fmt(poly.areaM2)} m²`));
    list.push(row('Volume', `${fmt(poly.volumeM3)} m³`));
    list.push('<hr/>');
    list.push(row('Kosten', costs));
    list.push(row('Opbrengst', opbrengst));
    if (scores) {
      list.push('<hr/>');
      list.push(row('Leefbaarheidsscore', scores.kengetallen ?? '-'));
    }
    if (llm) {
      list.push(row('LLM-score', llm.score ?? '-'));
      if (llm.motivatie) list.push(`<div class="muted">${escapeHtml(llm.motivatie)}</div>`);
    }
    if (warnings?.length) {
      list.push('<hr/>');
      list.push(`<div class="muted">Waarschuwingen:<br/>• ${warnings.map(escapeHtml).join('<br/>• ')}</div>`);
    }
    this.contentEl.innerHTML = list.join('');
  }

  showSimulation(resp) {
    const score = resp?.score ?? resp?.result?.score ?? '-';
    const motivatie = resp?.motivatie ?? resp?.result?.motivatie ?? '';
    this.simEl.innerHTML = `<div>Score: <b>${escapeHtml(String(score))}</b></div>${motivatie ? `<div class="muted">${escapeHtml(motivatie)}</div>` : ''}`;
  }
}

export class CostPanel {
  constructor(contentEl) {
    this.contentEl = contentEl;
  }

  clear() {
    if (this.contentEl) {
      this.contentEl.innerHTML = '<span class="muted">Selecteer een polygon...</span>';
    }
  }

  showCosts(poly) {
    if (!this.contentEl) return;
    const kg = KENGETALLEN[poly.type];
    const costs = calculateCosts(poly);
    if (!kg || !costs) {
      this.clear();
      return;
    }
    this.contentEl.innerHTML = `
      <div><span class="label">Type</span>: <span class="value">${kg.code}. ${escapeHtml(kg.label)}</span></div>
      <div><span class="label">Oppervlakte</span>: <span class="value">${fmt(poly.areaM2)} m2</span></div>
      <div><span class="label">Volume</span>: <span class="value">${fmt(poly.volumeM3)} m3</span></div>
      <hr/>
      <div><span class="label">Kosten</span>: <span class="value">${fmtCurrency(costs.kosten)}</span></div>
      <div><span class="label">Opbrengst</span>: <span class="value">${fmtCurrency(costs.opbrengst)}</span></div>
      <div><span class="label">Bewoners</span>: <span class="value">${costs.bewoners !== null ? fmt(costs.bewoners) : 'n.v.t.'}</span></div>
      <div><span class="label">Punten</span>: <span class="value">${fmt(Math.round(costs.punten * 100) / 100)}</span></div>
    `;
  }
}

export function bindControls(handlers) {
  const $ = (id) => document.getElementById(id);
  const btnDraw = $('btn-draw');
  const btnEdit = $('btn-edit');
  const btnDelete = $('btn-delete');
  const btnUndo = $('btn-undo');
  const btnRedo = $('btn-redo');
  const btnResetCamera = $('btn-reset-camera');
  const btnSimulate = $('btn-simulate');
  const selType = $('select-type');
  const rangeHeight = $('height-range');
  const numHeight = $('height-input');

  btnDraw.addEventListener('click', handlers.onDraw);
  btnEdit.addEventListener('click', handlers.onEdit);
  btnDelete.addEventListener('click', handlers.onDelete);
  btnUndo.addEventListener('click', handlers.onUndo);
  btnRedo.addEventListener('click', handlers.onRedo);
  btnResetCamera.addEventListener('click', handlers.onResetCamera);
  if (btnSimulate) btnSimulate.addEventListener('click', handlers.onSimulate);

  selType.addEventListener('change', () => handlers.onTypeChange(selType.value));

  const syncFromRange = () => { numHeight.value = rangeHeight.value; handlers.onHeightChange(Number(rangeHeight.value)); };
  rangeHeight.addEventListener('input', syncFromRange);
  numHeight.addEventListener('change', () => {
    const v = clamp(Number(numHeight.value), Number(numHeight.min), Number(numHeight.max));
    numHeight.value = String(v);
    rangeHeight.value = String(v);
    handlers.onHeightChange(v);
  });

  function syncStyle(type, height) {
    if (type) selType.value = type;
    if (typeof height === 'number') {
      const v = clamp(height, Number(numHeight.min), Number(numHeight.max));
      numHeight.value = String(v);
      rangeHeight.value = String(v);
    }
  }

  return { syncStyle };
}

function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }
function fmt(n) { return (n ?? 0).toLocaleString('nl-NL'); }
function fmtCurrency(n) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n);
}
function row(label, value) { return `<div><span class="label">${escapeHtml(label)}</span>: <span class="value">${escapeHtml(String(value))}</span></div>`; }
function labelForType(t) { return KENGETALLEN[t]?.label || t; }
function escapeHtml(s) { return s.replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
