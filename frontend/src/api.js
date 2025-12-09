export class ApiClient {
  constructor({ baseUrl = '' } = {}) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
    this.headers = { 'Content-Type': 'application/json' };
  }

  async createPolygon(payload) {
    return this._fetchJson('/polygon', { method: 'POST', body: JSON.stringify(payload) });
  }

  async updatePolygon(id, payload) {
    return this._fetchJson(`/polygon/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
  }

  async deletePolygon(id) {
    return this._fetchJson(`/polygon/${encodeURIComponent(id)}`, { method: 'DELETE' });
  }

  async getScenarioScores(id) {
    return this._fetchJson(`/scenario/${encodeURIComponent(id)}/scores`);
  }

  async runAgents(payload) {
    return this._fetchJson('/agents/run', { method: 'POST', body: JSON.stringify(payload || {}) });
  }

  async _fetchJson(path, init = {}) {
    const url = this.baseUrl + path;
    const res = await fetch(url, { ...init, headers: { ...this.headers, ...(init.headers || {}) } });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
    }
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      return res.json().catch(() => ({}));
    }
    // Niet‑JSON (bijv. leeg of tekst). Retourneer tekst voor debuggen of een leeg object als er niets is
    const text = await res.text().catch(() => '');
    return text || {};
  }
}
