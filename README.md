# Spoordok Digital Twin - Project Documentatie

Dit project is een interactieve "Digital Twin" van de Spoordok-omgeving in Leeuwarden, gebouwd met CesiumJS voor de visualisatie en een Spring Boot backend voor de data-opslag.

## Hoe het applicatie werkt

De applicatie stelt gebruikers in staat om de Spoordok-omgeving in 3D te bekijken en aan te passen.

### Belangrijkste functionaliteiten:
- **Teken Polygonen:** Hiermee kun je gebouwen, water of parken toevoegen. Na het tekenen opent een venster voor de naam en omschrijving.
- **Teken Wegen:** Hiermee kun je wegsegmenten toevoegen aan de kaart.
- **Bewerken:** Selecteer een object op de kaart om de hoogte (voor gebouwen), breedte (voor wegen) of de functie (bijv. van 'Wonen' naar 'Kantoor') aan te passen.
- **Roteren:** Geselecteerde gebouwen kunnen worden geroteerd met de pijltjestoetsen of de 'A' en 'D' toetsen. Houd de toets ingedrukt voor een soepele rotatie. De wijziging wordt pas naar de database gestuurd zodra je de toets loslaat.
- **Undo/Redo:** Acties zoals aanmaken, aanpassen of verwijderen kunnen ongedaan worden gemaakt met de knoppen of `Ctrl+Z` / `Ctrl+Y`.
- **Informatiepaneel:** Rechts zie je details van het geselecteerde object, inclusief berekende waarden zoals kosten, bewoners en punten op basis van het gebouwtype.

## Installatie en Opstarten (Docker)

De makkelijkste manier om de hele stack (frontend, backend en database) op te starten is met Docker Compose.

### Vereisten:
- Docker Desktop geïnstalleerd en draaiend.

### Stappen:
1. Open een terminal in de hoofdmap van het project.
2. Voer het volgende commando uit:
   ```bash
   docker-compose up --build
   ```
3. Zodra alle containers draaien, is de applicatie bereikbaar op:
   - **Frontend:** [http://localhost:6002](http://localhost:6002)
   - **Backend API:** [http://localhost:6001](http://localhost:6001)

## Testen van de applicatie

Om te verifiëren of alles correct werkt, kun je de volgende teststappen doorlopen:

### 1. Basisfuncties testen
- **Laden:** Controleer of de kaart van Leeuwarden (Spoordok) laadt en of bestaande gebouwen verschijnen.
- **Teken Polygon:** Klik op 'Teken polygon', plaats 3 of meer punten en dubbelklik (of klik op het eerste punt) om te sluiten. Vul het venster in en sla op.
- **Teken Weg:** Klik op 'Teken weg', plaats punten en dubbelklik om te stoppen.

### 2. Rotatie testen
- Selecteer een zelfgetekend of bestaand gebouw.
- Houd de `A` toets of `Pijltje Links` ingedrukt. Het gebouw moet soepel draaien.
- Laat de toets los en ververs de pagina. Controleer of het gebouw in de nieuwe stand is blijven staan (bevestiging dat de API-call gelukt is).

### 3. Bewerken en Verwijderen
- Verander het type van een gebouw via de dropdown aan de linkerkant.
- Pas de hoogte aan met de slider.
- Klik op 'Verwijder' om een object weg te halen.
- Gebruik 'Undo' om het verwijderde object weer terug te halen.

### 4. Backend Validatie (Optioneel)
- Je kunt de API direct testen via Swagger UI: [http://localhost:6001/swagger-ui.html](http://localhost:6001/swagger-ui.html)
- Controleer via de `/api/buildings/list` endpoint of je wijzigingen daar ook zichtbaar zijn in de JSON output.
