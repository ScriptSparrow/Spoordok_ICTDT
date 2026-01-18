
# Spoordock API

De spoordock API is een SpringBoot Web API die dient als de abstractie laag van de database en het LLM landschap.

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
