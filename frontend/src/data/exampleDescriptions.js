/**
 * Voorbeeldbeschrijvingen voor elk gebouwtype.
 * Sleutels komen overeen met de gebouwtype namen uit de database.
 */
export const exampleDescriptions = {
    "Vrijstaand huis": [
        "Moderne vrijstaande woning met ruime tuin en dubbele garage.",
        "Energiezuinige villa met zonnepanelen en warmtepomp.",
        "Klassieke vrijstaande woning met karakteristieke gevelelementen."
    ],
    "Rijtjeswoning": [
        "Rijtjeswoning met 3 slaapkamers en achtertuin.",
        "Compacte starterswoning met mogelijkheid tot uitbouw.",
        "Gerenoveerde tussenwoning met moderne keuken."
    ],
    "Appartement": [
        "Ruim appartement op de derde verdieping met balkon.",
        "Studio met open keuken, geschikt voor starters.",
        "Penthouse met dakterras en panoramisch uitzicht."
    ],
    "Bedrijfsgebouw": [
        "Kantoorpand met flexibele werkplekken en vergaderruimtes.",
        "Bedrijfshal met laadkade en kantoorruimte.",
        "Gemengd bedrijfspand geschikt voor lichte industrie."
    ],
    "Park/groen": [
        "Openbaar park met speeltoestellen en wandelpaden.",
        "Groene buffer tussen woonwijk en bedrijventerrein.",
        "Natuurlijke groenstrook met inheemse beplanting."
    ],
    "Wegen": [
        "Hoofdontsluitingsweg met gescheiden fietspaden.",
        "Woonerf met snelheidsremmende maatregelen.",
        "Verbindingsweg tussen wijken met bushalte."
    ],
    "Parkeerplaatsen": [
        "Openbaar parkeerterrein met 50 plaatsen.",
        "Bezoekersparkeren bij winkelcentrum.",
        "Parkeerzone voor bewoners met vergunning."
    ],
    "Parkeerplaatsen overdekt": [
        "Ondergrondse parkeergarage met 100 plaatsen.",
        "Overdekte fietsenstalling bij station.",
        "Parkeerkelder onder appartementencomplex."
    ]
};

/**
 * Haalt een willekeurige voorbeeldbeschrijving op voor een gebouwtype.
 * @param {string} buildingType - De naam van het gebouwtype
 * @returns {string} Een willekeurige voorbeeldbeschrijving of standaardtekst
 */
export function getExampleDescription(buildingType) {
    const descriptions = exampleDescriptions[buildingType];
    if (descriptions && descriptions.length > 0) {
        const randomIndex = Math.floor(Math.random() * descriptions.length);
        return descriptions[randomIndex];
    }
    return "Voer hier een beschrijving in voor dit object.";
}
