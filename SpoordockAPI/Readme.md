
# Spoordock API

De spoordock API is een SpringBoot Web API die dient als de abstractie laag van de database en het LLM landschap.

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
 
   run `ollama list` om alle modellen te zien.

4. Hierna is de applicatie beschikbaar op: 
        http://localhost:6002

   De swagger pagina met alle endpoints is beschikbaar op:
        http://localhost:6001/swagger


## Dockerfile

### Targets

De Dockerfile heeft "targets". <br>
Dit zijn:

- `release` : volledig gecompileerde jar op een jre image.
- `dev` : jdk image met ongecompileerde code. Start via "mvn spring-boot:run"
        en zorgt voor hot reloads en de java debugger op poort 5005.

## Development en Debuggen

Development en debuggen wordt gedaan in de docker containers met behulp van de docker-compose bestanden. <br>
Dit is om er voor te zorgen dat we allemaal met dezelfde dependencies en constraints werken.

### Hot Reloads

Voor alle IDEs zijn hot reloads geconfigureerd via de sprint devtools.
Dit herstart the java applicatie na een aanpassing van een bestand (opslaan).
Dit kan some 2 a 3 seconden duren.



### Visual Studio

[launch.json](../.vscode/launch.json) : Heeft de launch configuraties.

`Debug (Attach) - SpoordockAPI` start the docker-compose.debug-api.yml en attached een java debugger aan de pod.

### Intellij

[./run](./run) : Deze folder heeft de taken. 

`Attach.run.xml` "Attached" de java debugger aan de pod. <br>
De pod wordt gestart door de `Start Docker Compose.run.xml` taak waar de `Attach.run.xml` taak afhankelijk van is.

## Development guidelines

### Code

- DI via constructors. NIET via property injection.
