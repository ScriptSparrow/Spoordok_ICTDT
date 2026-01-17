package nhl.stenden.spoordock.llmService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;

@ExtendWith(MockitoExtension.class)
public class OllamaEmbeddingClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private LlmConfiguration configuration;

    @Mock
    private HttpResponse<String> httpResponse;

    private OllamaEmbeddingClient client;

    @BeforeEach
    public void setUp() {
        when(configuration.getBaseUrl()).thenReturn("http://localhost:11434");
        client = new OllamaEmbeddingClient(configuration, httpClient);
    }

    @Test
    public void testCreateEmbedding() throws Exception {
        String responseBody = "{\"embedding\":[[0.1,0.2,0.3]]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        float[] result = client.createEmbedding("test text");

        assertNotNull(result);
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result, 0.001f);
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testCreateEmbedding_NonSuccessStatusCode() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.createEmbedding("test text");
        });

        assertTrue(exception.getMessage().contains("Failed to create embedding"));
    }

    @Test
    public void testCreateEmbedding_HttpClientException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new RuntimeException("Network error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.createEmbedding("test text");
        });

        assertTrue(exception.getMessage().contains("Failed to create embedding"));
    }

    @Test
    public void testGetEmbeddingModelName() {
        assertEquals("nomic-embed-text", client.getEmbeddingModelName());
    }

    @Test
    public void testCreateEmbedding_InvalidJsonResponse() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("invalid json");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.createEmbedding("test text");
        });

        assertTrue(exception.getMessage().contains("Failed to create embedding"));
    }
}
