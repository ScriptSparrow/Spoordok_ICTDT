 # Spoordok Digital Twin
> **Opdrachtgever**: Gemeente Leeuwarden  
> **Periode**: 2025-2026 P2 (Week 1-8)  
> **Opleiding**: NHL Stenden HBO-ICT Jaar 2

> **Github Projects**: [Kanban Board](https://github.com/users/ScriptSparrow/projects/2)


---

## Project Beschrijving

Development van een Digital Twin voor het Spoordok gebied in Leeuwarden. De simulatie ondersteunt de gemeente bij herontwikkelingsbesluiten door effecten van veranderingen in bebouwing, groenvoorziening en infrastructuur inzichtelijk te maken.

---

## Tech Stack

**Backend**: 
* Java 21, 
* Spring Boot, 
* Maven

**Frontend**: 
* CesiumJS, 
* JavaScript ES6+

**Infrastructure**: 
* Docker, 
* Linux (Ubuntu)

**Database**: 
* PostgreSQL

**Version Control**: 
* Git, 
* GitHub

---

## Repository Structuur
```
spoordok-icdt/
├── SpoordockAPI/                  # Spring Boot API
│   ├── src/                  # Java/Kotlin code
│   ├── resources/            # Configuratiebestanden
│   └── tests/                # Unit/integration tests voor backend (JUnit)
├── frontend/                 # CesiumJS visualization
│   ├── src/                  # Frontend code (JavaScript/Cesium)
│   ├── public/               # Statische assets
│   └── tests/                # Unit/e2e tests voor frontend (Jest of Cypress)
├── docs/                     # Documentatie en rapporten
└── docker/                   # Container configuratie

```

---

## SpoordockAPI

[Readme here](./SpoordockAPI/Readme.md)


## Docker Compose

[docker-compose.base.yml](docker-compose.base.yml) :
    De basis elementen. 

[docker-compose.debug-api.yml](docker-copmose.debug.yml):


## Frontend Development (Vite + Proxy naar Backend) — Optie A

Deze repo gebruikt de volgende, meest gebruikelijke werkwijze voor lokale ontwikkeling:

- Frontend draait met Vite (poort 5173) in map `frontend/`.
- Backend (Spring Boot) draait op poort 8080 in map `SpoordockAPI/`.
- De frontend roept API’s aan via pad `/api/...`, dat door Vite wordt geproxied naar de backend op `http://localhost:8080`.

### Eenmalige setup
1. Node.js 18+ installeren.
2. In `frontend/` dependencies installeren:
   - `cd frontend`
   - `npm ci`
3. `.env` aanmaken op basis van voorbeeld:
   - Kopieer `frontend/.env.example` naar `frontend/.env`.
   - Vul `VITE_CESIUM_ION_TOKEN` met jouw Cesium Ion token.

### Project starten (lokaal)
- Backend:
  - `cd SpoordockAPI`
  - `./mvnw spring-boot:run` (of run via IntelliJ/IDEA)
- Frontend (in een apart terminalvenster):
  - `cd frontend`
  - `npm run dev`
  - Open http://localhost:5173

De Vite dev server bevat een proxy:
- Requests naar `/api/...` gaan automatisch naar `http://localhost:8080/...`.
- Hierdoor is CORS-config niet nodig tijdens development.

### Omgevingsvariabelen (frontend)
- `VITE_API_BASE=/api` (via `.env`)
- `VITE_CESIUM_ION_TOKEN=<jouw_token>`

Deze zijn beschikbaar in de browser via `import.meta.env.VITE_*`.

### Veelvoorkomende issues & oplossingen
- MIME type error bij het laden van modules (bijv. `/src/viewer.js`):
  - Zorg dat je Vite start vanuit de map `frontend/` (niet vanuit de projectroot).
  - In `frontend/index.html` gebruiken we absolute paden: `/src/app.js` en `/src/ui.css`.
  - Hard refresh (Ctrl/Cmd+Shift+R) en check in DevTools → Network of `/src/app.js` en `/src/viewer.js` status 200 + Content-Type `application/javascript` teruggeven.
- API requests falen met CORS:
  - Gebruik de proxy (Optie A): `VITE_API_BASE=/api` en start Vite; geen extra CORS-werk nodig.
  - Als je toch een directe URL wil gebruiken (zonder proxy), zet `VITE_API_BASE=http://localhost:8080` en zorg voor CORS in Spring Boot.

### Productie build
- `cd frontend && npm run build` → output in `frontend/dist/`.
- Optie productie hosting:
  - A) Kopieer `dist/` naar `SpoordockAPI/src/main/resources/static/` zodat Spring Boot frontend serveert (zelfde origin, geen CORS nodig).
  - B) Host frontend apart (Nginx) en routeer `/api` naar backend via reverse proxy.

---

## Quick Links

[Plan van Aanpak](https://newuniversity-my.sharepoint.com/:w:/r/personal/garik_sandrosyan1_student_nhlstenden_com/Documents/HBO-ICT%202.2%20Groep%201%20-%20Digital%20Twin%20Plan%20van%20Aanpak.docx?d=w1decf2bfe00b426eb7c3a10f7914f11a&csf=1&web=1&e=5jKQ8w)

[GitKraken](https://www.gitkraken.com/)
---


```

---

**Last Updated**: 09 December 2025


---

## Uitleg Polygon Editor (front-end)

De editor is ontworpen voor niet-technische gebruikers en werkt als een simpele city‑builder toolset. Hieronder de belangrijkste handelingen.

- Tekenen
  - Klik op "Teken polygon".
  - Klik op de kaart om punten te plaatsen.
  - Klik dicht bij het eerste punt of klik met de rechtermuisknop om de polygon te sluiten (minimaal 3 punten).
  - Tijdens het tekenen zie je een witte voorvertoningslijn.

- Bewerken
  - Klik op "Bewerken".
  - Klik op een polygon om deze te selecteren.
  - Versleep witte handles (rondjes) om losse vertices te verplaatsen.
  - Klik op de polygon (buiten de handles) en sleep om het geheel te verplaatsen.

- Eigenschappen (linkerpaneel)
  - Kies een functietype (kleur wordt automatisch aangepast): Groen/Water/Wonen/Industrie/Kantoor.
  - Stel de hoogte in (slider of numerieke invoer). Oppervlakte (m²) en volume (m³) worden automatisch herberekend.

- Undo/Redo en sneltoetsen
  - Undo: Ctrl+Z
  - Redo: Ctrl+Y
  - Annuleren huidig tekenen: Esc
  - Verwijderen geselecteerde polygon: Delete

- Verwijderen
  - Klik op "Verwijder" of gebruik de Delete‑toets bij een geselecteerde polygon.

- Opslaan en bijwerken (backend)
  - Bij aanmaken: de polygon wordt via `POST /polygon` verstuurd.
  - Bij bewerken: wijzigingen gaan via `PUT /polygon/{id}`.
  - Bij verwijderen: `DELETE /polygon/{id}`.
  - Payload bevat o.a. type, hoogte, oppervlakte/volume en `geometry.coordinates` als [lon,lat,height].

- Informatiepanel (rechts)
  - Selecteer een polygon om details te zien (functie, hoogte, oppervlakte, volume).
  - Indien `scenarioId` bekend is, worden scores opgehaald via `GET /scenario/{id}/scores` en getoond.

- Simulatie
  - Klik op "Simuleer leefkwaliteit" om `POST /agents/run` aan te roepen.
  - De score en eventuele motivatie worden onder de knop getoond.

### Troubleshooting (frontend)
- Als Cesium assets (Widgets/Assets/Workers) 404 of `text/html` teruggeven:
  - Herstart de Vite dev server en doe een harde refresh.
  - Controleer of de assets onder `/Cesium/...` bereikbaar zijn. `window.CESIUM_BASE_URL = '/Cesium'` staat in `src/viewer.js`.
- Als API‑calls falen door CORS:
  - Gebruik de meegeleverde Vite proxy (`VITE_API_BASE=/api`) en draai de backend op poort 8080.
