// Kengetallen configuratie en berekeningen voor kostenberekening
// Bron: Gemeente Leeuwarden Digital Twin specificaties

export const KENGETALLEN = {
  A: {
    code: 'A',
    label: 'Vrijstaand huis',
    eenheid: 'm3',
    kosten: 500,        // per m3
    opbrengstPct: 0.12, // 12%
    bewoners: 0.005,    // bewoners per m3
    punten: 4,          // punten voor natuur/leefbaarheid per eenheid
    color: '#f97316',
  },
  B: {
    code: 'B',
    label: 'Rijtjeswoning',
    eenheid: 'm3',
    kosten: 400,
    opbrengstPct: 0.08,
    bewoners: 0.01,
    punten: 6,
    color: '#fbbf24',
  },
  C: {
    code: 'C',
    label: 'Appartement',
    eenheid: 'm3',
    kosten: 300,
    opbrengstPct: 0.12,
    bewoners: 0.006,
    punten: 5,
    color: '#eab308',
  },
  D: {
    code: 'D',
    label: 'Bedrijfsgebouw',
    eenheid: 'm3',
    kosten: 200,
    opbrengstPct: 0.15,
    bewoners: 0.018,    // medewerkers per m3
    punten: 2,
    color: '#9ca3af',
  },
  E: {
    code: 'E',
    label: 'Park/groen',
    eenheid: 'm2',
    kosten: 150,
    opbrengstPct: 0,
    bewoners: null,     // n.v.t.
    punten: 10,
    color: '#34d399',
  },
  F: {
    code: 'F',
    label: 'Wegen',
    eenheid: 'm2',
    kosten: 100,
    opbrengstPct: 0.05,
    bewoners: null,
    punten: 8,
    color: '#64748b',
  },
  G: {
    code: 'G',
    label: 'Parkeerplaatsen',
    eenheid: 'm2',
    kosten: 100,
    opbrengstPct: 0.10,
    bewoners: null,
    punten: 6,
    color: '#6b7280',
  },
  H: {
    code: 'H',
    label: 'Parkeerplaatsen overdekt',
    eenheid: 'm2',
    kosten: 1500,
    opbrengstPct: 0.15,
    bewoners: null,
    punten: 10,
    color: '#a78bfa',
  },
};

/**
 * Bereken kosten, opbrengst, bewoners en punten voor een polygon
 * @param {Object} poly - Polygon object met type, areaM2, volumeM3
 * @returns {Object|null} Berekende waarden of null als type onbekend
 */
export function calculateCosts(poly) {
  const kg = KENGETALLEN[poly.type];
  if (!kg) return null;

  // Bepaal basiswaarde (m2 of m3) afhankelijk van type
  const basiswaarde = kg.eenheid === 'm3' ? poly.volumeM3 : poly.areaM2;

  // Bereken waarden
  const kosten = basiswaarde * kg.kosten;
  const opbrengst = kosten * kg.opbrengstPct;
  const bewoners = kg.bewoners !== null ? Math.round(basiswaarde * kg.bewoners) : null;
  const punten = (basiswaarde * kg.punten) / 100; // geschaald voor redelijke getallen

  return {
    kosten,
    opbrengst,
    bewoners,
    punten,
    eenheid: kg.eenheid,
    basiswaarde,
  };
}

/**
 * Krijg kleurcode voor een type
 * @param {string} typeCode - Type code (A-H)
 * @returns {string} Hex kleurcode
 */
export function getTypeColor(typeCode) {
  return KENGETALLEN[typeCode]?.color || '#ffffff';
}