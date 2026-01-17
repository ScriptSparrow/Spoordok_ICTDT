# CHANGELOG

ADD | frontend/src/api/featuresApi.js: getBuildingTypes methode toegevoegd om gebouwtypes uit de backend op te halen.
CHG | frontend/src/api/featuresApi.js: Bugfix in delete methode (ontbrekende baseUrl toegevoegd).
CHG | frontend/src/api/featuresApi.js: create en update methoden aangepast om buildingTypeId mee te sturen.
CHG | frontend/src/editor/cesiumEditor.js: finishDrawing, updateSelectedFeature en deleteSelected zijn nu asynchroon (await).
CHG | frontend/src/editor/cesiumEditor.js: Gebouwtypes worden nu gemapt op basis van de database UUID's.
CHG | frontend/src/editor/cesiumEditor.js: Dropdown menu voor types wordt nu dynamisch gevuld vanuit de API.
CHG | frontend/src/editor/cesiumEditor.js: Verbeterde foutafhandeling met meldingen naar de gebruiker.
CHG | frontend/src/app.js: Initialisatie aangepast om gebouwtypes op te halen bij het starten.
CHG | BuildingTypeEntity.java & BuildingTypeDTO.java: Type van costPerUnit veranderd van String naar double om overeen te komen met de database.
CHG | BuildingPolygonEntity.java: Kolomdefinitie voor punten aangepast naar geometry(PolygonZ, 4326) voor 3D coördinaten.
CHG | BuildingPolygonEntity.java: description kolom gemarkeerd als nullable = false.
CHG | BuildingService.java: addBuilding en updateBuilding halen nu de volledige BuildingTypeEntity op uit de repository voor betere validatie.
CHG | BuildingPolygonMapper.java: Mapping logica vereenvoudigd; koppeling met gebouwtype wordt nu door de service gedaan.
CHG | BuildingTypeMapper.java: Verwerkt costPerUnit nu als numerieke waarde (double).
CHG | BuildingTypeEntity.java & BuildingTypeDTO.java: points aangepast naar Integer om NULL waarden uit de database te ondersteunen.
CHG | BuildingPolygonEmbeddingEntity.java: @Table annotatie toegevoegd met de juiste tabelnaam (polygon_embeddings).
CHG | BuildingPolygonEmbeddingRepository.java: Tabel- en kolomnamen gecorrigeerd in de native queries.
CHG | docker-compose.base.yml: Ongeldige depends_on syntax verwijderd bij de liquibase service.
CHG | Project-breed: Nederlandse commentaren toegevoegd aan alle gewijzigde code bestanden.
CHG | docker-compose.yml: Configuratie bijgewerkt voor database connectiviteit.
ADD | .liquibase/changelogs/changelog.1.2.yml: Nieuwe migratie toegevoegd om `omschrijving` kolom aan `wegsegmenten` tabel toe te voegen.
CHG | .liquibase/changelog-root.yml: Referentie naar changelog.1.2.yml toegevoegd.
CHG | RoadSegment.java: `roadDescription` veld correct gemapped naar `omschrijving` kolom. `width` veld toegevoegd voor `breedte` kolom. Constructor bijgewerkt.
CHG | RoadSegementDTO.java: `width` veld toegevoegd voor breedte van wegsegmenten. Constructor bijgewerkt.
CHG | RoadSegmentMapper.java: Mapping bijgewerkt om `width` veld te verwerken in toDTO en toEntity methodes.
CHG | cesiumEditor.js: Dropdown default waarde aangepast van hardcoded 'housing' naar eerste beschikbare optie uit de database.
CHG | BuildingPolygonEntity.java: `nullable = false` toegevoegd aan @JoinColumn voor gebouwtype om overeen te komen met database constraint.
CHG | BuildingPolygonEntity.java: `nullable = false` toegevoegd aan @Column voor punten (polygon) om overeen te komen met database constraint.
CHG | BuildingService.java: `@Transactional` annotatie toegevoegd aan addBuilding en updateBuilding methodes.
CHG | BuildingService.java: `ifPresent` vervangen door `orElseThrow` voor betere foutafhandeling bij ontbrekend of ongeldig gebouwtype.
CHG | BuildingService.java: `scheduleEmbeddingTask` aangepast om alleen UUID te accepteren in plaats van hele entity (voorkomt StaleObjectStateException).
CHG | BuildingService.java: Achtergrondtaak haalt nu verse data op uit database met eager-loaded relaties.
CHG | BuildingService.java: Null-check toegevoegd voor wanneer een gebouw ondertussen is verwijderd.
ADD | BuildingPolygonRepository.java: `findByIdIncludingBuildingType` methode toegevoegd voor eager fetch van gebouw met gebouwtype.
CHG | BuildingPolygonEntity.java: Implementeert nu `Persistable<UUID>` interface om StaleObjectStateException te voorkomen bij frontend-gegenereerde UUID's.
CHG | BuildingPolygonEntity.java: `@GeneratedValue` annotatie verwijderd; frontend genereert de UUID.
CHG | BuildingPolygonEntity.java: `@Transient` veld `isNew` toegevoegd om bij te houden of entity nieuw is.
CHG | BuildingPolygonEntity.java: `@PostLoad` en `@PostPersist` callbacks toegevoegd om isNew status te beheren.
CHG | BuildingPolygonEntity.java: Expliciete constructor toegevoegd die `isNew` op true zet voor nieuwe entities.
CHG | BuildingService.java: `updateBuilding()` haalt nu eerst de bestaande entity op uit de database voordat velden worden bijgewerkt (voorkomt INSERT bij UPDATE door Persistable interface).
CHG | cesiumEditor.js: `syncEntity()` gebruikt nu de kleur uit de database (`meta.color`) als die beschikbaar is, met fallback naar TYPE_COLORS.
CHG | cesiumEditor.js: `updateEntityHighlight()` gebruikt nu de kleur uit de database (`meta.color`) als die beschikbaar is, met fallback naar TYPE_COLORS.
CHG | cesiumEditor.js: `updateSelectedFeature()` kopieert nu ook de `color` eigenschap naar meta-data bij type-wijziging, zodat de polygoon direct de juiste kleur krijgt.
