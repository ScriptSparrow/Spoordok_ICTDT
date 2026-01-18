## Start

Om het project te laten draaien moet je de volgende stappen zetten: 

1. `docker compose up --build --detach`
    Wacht tot de docker containers draaien.
    Indien `.db/.db_data` al bestaat is het het best om deze te verwijderen. 
    Hier kan nog oude data in zitten.

2. `docker exec -ti ollama /bin/sh`
   Hiermee start je een sessie met ollama

3. `ollama pull <model>`

   required: 
     - `nomic-embed-text `

   geteste modellen: 
     - `qwen3:8b`
    (kleinere modellen gaan niet goed om met de benodigde tool calls.)

   run `ollama list` om alle modellen te zien.

4. Hierna is de applicatie beschikbaar op: 
        http://localhost:6002

   De swagger pagina met alle endpoints is beschikbaar op:
        http://localhost:6001/swagger

