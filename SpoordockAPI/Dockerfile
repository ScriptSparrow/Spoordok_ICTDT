
# Add an extra dev target
FROM maven:3.9.6-eclipse-temurin-21 AS dev
WORKDIR /workspace
# Copy pom + download deps first for caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
# Copy sources
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests clean install
ENV SERVER_PORT=6001
EXPOSE 6001 5005
CMD ["mvn","-B","spring-boot:run", \
    "-Dspring-boot.run.fork=false", \
    "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
    "-Dspring.devtools.restart.enabled=true"]


FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS release
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SERVER_PORT=6001
EXPOSE 6001
ENTRYPOINT ["java", "-jar", "app.jar"]
