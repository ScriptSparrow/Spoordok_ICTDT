package nhl.stenden.spoordock.llmService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingRequestBody;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingResponseBody;
import nhl.stenden.spoordock.llmService.dtos.GenerateRequest;
import nhl.stenden.spoordock.llmService.dtos.GenerateResponse;
import nhl.stenden.spoordock.llmService.dtos.ChatRequest;
import nhl.stenden.spoordock.llmService.dtos.ChatResponse;
import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.classes.HistoricalMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.Role;

@Slf4j
@Component
public class OllamaConnectorService {

    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IChatHistoryManager historyManager;
    private final LlmConfiguration.SystemPrompts systemPrompts;

    /**
     * Constructs a new OllamaConnectorService with the given configuration and history manager.
     *
     * @param configuration  the LLM configuration containing the base URL and system prompts
     * @param historyManager the chat history manager for storing conversation history
     */
    public OllamaConnectorService(LlmConfiguration configuration, IChatHistoryManager historyManager) {
        String apiUrl = configuration.getBaseUrl();
        this.baseUrl = URI.create(apiUrl).normalize();
        this.historyManager = historyManager;
        this.systemPrompts = configuration.getSystemPrompts();
    }

    /**
     * Returns the name of the embedding model used for creating text embeddings.
     *
     * @return the embedding model name
     */
    public String getEmbeddingModelName() {
        return EMBEDDING_MODEL_NAME;
    }


    /**
     * Creates a vector embedding for the given text using the Ollama embedding API.
     *
     * @param text the text to create an embedding for
     * @return a float array representing the text embedding vector
     * @throws RuntimeException if the embedding creation fails
     */
    public float[] createEmbedding(String text) {
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


    /**
     * Streams a description helper response for the given prompt using the configured description helper system prompt.
     * Each chunk of the response is passed to the provided consumer as it arrives.
     *
     * @param chatId        the unique identifier for the chat session
     * @param prompt        the user's prompt/question
     * @param model         the LLM model to use for generation
     * @param chunkReceived a consumer that receives each text chunk as it streams in
     */
    public void generateDescriptionHelperStream(UUID chatId, String prompt, String model, Consumer<String> chunkReceived) {
        chatStream(chatId, prompt, systemPrompts.getDescriptionHelperPrompt(), model, chunkReceived);
    }

    
    /**
     * Streams a text generation response using the Ollama /api/generate endpoint.
     * This method is for single-turn generation without conversation history.
     *
     * @param prompt        the user's prompt/question
     * @param systemPrompt  the system prompt that defines the AI's behavior
     * @param model         the LLM model to use for generation
     * @param chunkReceived a consumer that receives each text chunk as it streams in
     * @throws RuntimeException if the generation fails or streaming encounters an error
     */
    private void generateStream(String prompt, String systemPrompt, String model, Consumer<String> chunkReceived) {
        try{
            GenerateRequest request = new GenerateRequest(model, prompt, systemPrompt, true);
            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(baseUrl.resolve("/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            // Stream the response
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    GenerateResponse chunk = objectMapper.readValue(line, GenerateResponse.class);
                    if (chunk.getResponse() != null) {
                        chunkReceived.accept(chunk.getResponse());  // <-- This calls back to your controller
                    }
                }

                
            }catch(Exception ex)
            {
                throw new RuntimeException("Error reading streamed response", ex);
            }

        }catch(Exception ex)
        {
            throw new RuntimeException("Failed to generate text stream", ex);
        }
    }


    /**
     * Streams a chat response using conversation history stored in the history manager.
     * The system prompt is used to initialize new conversations, and the assistant's response
     * is automatically saved to the conversation history.
     *
     * @param chatId        the unique identifier for the chat session
     * @param prompt        the user's prompt/question
     * @param systemPrompt  the system prompt that defines the AI's behavior (used for new conversations)
     * @param model         the LLM model to use for generation
     * @param chunkReceived a consumer that receives each text chunk as it streams in
     * @throws RuntimeException if the chat stream fails or an error occurs during streaming
     */
    private void chatStream(UUID chatId, String prompt, String systemPrompt, String model, Consumer<String> chunkReceived) {
         try{
            HistoricalMessage systemMessage = new HistoricalMessage(Role.SYSTEM, systemPrompt);
            historyManager.createHistoryIfNotExists(chatId, systemMessage);

            HistoricalMessage userMessage = new HistoricalMessage(Role.USER, prompt);
            historyManager.addMessageToHistory(chatId, userMessage);

            List<HistoricalMessage> history = historyManager.getHistory(chatId, 20);
            List<ChatRequest.Message> messages = history.stream()
                .map(msg -> new ChatRequest.Message(msg.getRole().toString().toLowerCase(), msg.getContent()))
                .toList();
            ChatRequest request = new ChatRequest(model, messages);

            String json = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(baseUrl.resolve("/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            // Stream the response
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            
            String fullReceived = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ChatResponse chunk = objectMapper.readValue(line, ChatResponse.class);
                    if (chunk.getMessage() != null) {
                        chunkReceived.accept(chunk.getMessage().getContent());  // <-- This calls back to the caller
                        fullReceived += chunk.getMessage().getContent();
                    }
                }

                HistoricalMessage message = new HistoricalMessage(Role.ASSISTANT, fullReceived);
                historyManager.addMessageToHistory(chatId, message);
                
            }catch(Exception ex)
            {
                throw new RuntimeException("Error reading streamed response", ex);
            }

        }catch(Exception ex)
        {
            throw new RuntimeException("Failed to generate text stream", ex);
        }
    }

}
