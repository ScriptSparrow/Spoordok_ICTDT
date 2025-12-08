package nhl.stenden.spoordock.llmService;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingRequestBody;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingResponseBody;

@Slf4j
@Component
public class OllamaConnectorService {

    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaConnectorService(OllamaConnectorConfiguration configuration){

        String apiUrl = configuration.getApiUrl();
        this.baseUrl = URI.create(apiUrl).normalize();
    }

    public String getEmbeddingModelName() {
        return EMBEDDING_MODEL_NAME;
    }


    public <T> float[] createEmbedding(T data, String text) {
        try{
            var body = new EmbeddingRequestBody(EMBEDDING_MODEL_NAME, text);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.resolve("/api/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            StopWatch sw = new StopWatch();
            sw.start();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {

                throw new RuntimeException("Failed to create embedding. Status code: " + response.statusCode() + ", Body: " + response.body());
            }

            sw.stop();
            log.info("Embedding creation took: " + sw.getTotalTimeMillis() + " ms");

            EmbeddingResponseBody responseBody = objectMapper.readValue(response.body(), EmbeddingResponseBody.class);
            return responseBody.getEmbedding()[0];
        }
        catch(Exception ex)
        {
            throw new RuntimeException("Failed to create embedding", ex);
        }
    }

    public InputStream generateTextStream(String prompt) {
        return generateTextStream(prompt, UUID.randomUUID());
    }

    public InputStream generateTextStream(String prompt, UUID contextId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    

    

}
