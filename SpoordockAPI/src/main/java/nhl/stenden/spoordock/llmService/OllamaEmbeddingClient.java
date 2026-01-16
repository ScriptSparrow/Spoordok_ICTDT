package nhl.stenden.spoordock.llmService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingRequestBody;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingResponseBody;

@Slf4j
@Component
public class OllamaEmbeddingClient {

    private final HttpClient httpClient;
    private final URI baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";

    public OllamaEmbeddingClient(LlmConfiguration configuration, HttpClient httpClient) {
        this.baseUrl = URI.create(configuration.getBaseUrl()).normalize();
        this.httpClient = httpClient;
    }
    
    public String getEmbeddingModelName() {
        return EMBEDDING_MODEL_NAME;
    }

    public float[] createEmbedding(String text) {
        try {
            var body = new EmbeddingRequestBody(EMBEDDING_MODEL_NAME, text);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.resolve("/api/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            StopWatch sw = new StopWatch();
            sw.start();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            sw.stop();

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to create embedding. Status code: " + response.statusCode() + ", Body: " + response.body());
            }

            log.info("Embedding creation took: {} ms", sw.getTotalTimeMillis());

            EmbeddingResponseBody responseBody = objectMapper.readValue(response.body(), EmbeddingResponseBody.class);
            return responseBody.getEmbedding()[0];
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create embedding", ex);
        }
    }
}