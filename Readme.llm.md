# 3D Building Modelling Tool — LLM Integration

A 3D modelling tool where users define buildings as polygons, assign purposes (grocery store, music hall, apartments, etc.), and interact with the data using natural language queries powered by a local LLM.

## Features

| Feature | Description |
|---------|-------------|
| **Natural Language Queries** | Ask questions like *"Is there a grocery store near apartment block X?"* or *"How many buildings have purpose Y?"* |
| **Description Advisor** | Real-time guidance on writing better building descriptions for improved LLM understanding |
| **Planning Suggestions** | *(Planned)* LLM-powered layout and placement recommendations |

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────────┐
│  Frontend   │────▶│  Java API   │────▶│  PostGIS + pgvector │
│  (3D Tool)  │     │             │     └─────────────────────┘
└─────────────┘     └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Ollama    │
                    └─────────────┘
```

All LLM functionality runs directly from Java—no separate Python service required.

## Tech Stack

- **Java 17+** with Spring Boot
- **PostGIS** with **pgvector** extension
- **Ollama** (local LLM inference)
- **LangChain4j** (Java LLM orchestration)

## Setup

### 1. Database — Enable pgvector

```sql
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE buildings ADD COLUMN description_embedding vector(384);
```

### 2. Dependencies (Maven)

```xml
<dependencies>
    <!-- LangChain4j + Ollama -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>0. 35.0</version>
    </dependency>
    <dependency>
        <groupId>dev. langchain4j</groupId>
        <artifactId>langchain4j-ollama</artifactId>
        <version>0.35.0</version>
    </dependency>
    
    <!-- pgvector JDBC -->
    <dependency>
        <groupId>com.pgvector</groupId>
        <artifactId>pgvector</artifactId>
        <version>0.1.6</version>
    </dependency>
</dependencies>
```

### 3.  Docker Compose

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: buildings
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata:/var/lib/postgresql/data

  ollama:
    image: ollama/ollama:latest
    volumes:
      - ollama_data:/root/.ollama
    # Uncomment for GPU support:
    # deploy:
    #   resources:
    #     reservations:
    #       devices:
    #         - capabilities: [gpu]

volumes:
  pgdata:
  ollama_data:
```

### 4. Pull Ollama Models

```bash
docker exec -it ollama ollama pull llama3.1
docker exec -it ollama ollama pull nomic-embed-text
```

## Implementation

### LLM Service

```java
@Service
public class LLMService {

    private final OllamaChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;

    public LLMService(@Value("${ollama.base-url}") String baseUrl) {
        this.chatModel = OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName("llama3.1")
            .build();

        this.embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .modelName("nomic-embed-text")
            .build();
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    public String chat(String prompt) {
        return chatModel. generate(prompt);
    }
}
```

### Semantic + Spatial Query

```java
public List<Building> findRelevantBuildings(String question, String nearBuilding) {
    float[] embedding = llmService.embed(question);
    
    return jdbcTemplate.query("""
        SELECT id, name, purpose, description, ST_AsText(geometry) as geometry,
               1 - (description_embedding <=> ? ::vector) as similarity
        FROM buildings
        WHERE 1 - (description_embedding <=> ?::vector) > 0.5
          AND ST_DWithin(geometry, 
              (SELECT geometry FROM buildings WHERE name = ? ), 500)
        ORDER BY similarity DESC
        LIMIT 10
        """,
        new BeanPropertyRowMapper<>(Building.class),
        new PGvector(embedding), new PGvector(embedding), nearBuilding
    );
}
```

### Description Advisor

```java
public DescriptionAdvice adviseDescription(String description, String purpose) {
    String prompt = """
        You are an expert at writing building descriptions for a 3D planning tool.
        
        Current description: "%s"
        Building purpose: %s
        
        Respond with JSON:
        {
          "score": <1-10>,
          "suggestions": ["suggestion1", "suggestion2"],
          "improved_description": "..."
        }
        """.formatted(description, purpose);

    String response = llmService.chat(prompt);
    return objectMapper.readValue(response, DescriptionAdvice.class);
}
```

### Embedding on Save

```java
@Service
public class BuildingService {

    public Building save(Building building) {
        // Generate embedding for searchability
        float[] embedding = llmService.embed(
            building.getName() + " " + 
            building.getPurpose() + " " + 
            building.getDescription()
        );
        building.setDescriptionEmbedding(embedding);
        
        return buildingRepository.save(building);
    }
}
```

## Query Examples

| Question | How it works |
|----------|--------------|
| *"Is there a grocery store near Block A?"* | Embeds question → semantic search for "grocery store" → spatial filter within 500m of Block A |
| *"How many residential buildings do we have?"* | Embeds question → counts matches above similarity threshold |
| *"Describe the area around the music hall"* | Finds music hall → retrieves nearby buildings → LLM generates summary |

## Tips

- **Debounce description advice** — wait 500ms after typing stops before calling LLM
- **Cache embeddings** — only regenerate when description changes
- **Combine queries** — pgvector + PostGIS lets you filter by meaning AND location in one query

## Roadmap

- [x] Natural language building queries
- [x] Description writing advisor
- [ ] Planning suggestions (optimal placement)
- [ ] Multi-building relationship analysis

## References

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [pgvector-java](https://github.com/pgvector/pgvector-java)
- [Ollama](https://ollama.com/)
- [PostGIS](https://postgis. net/)