package nhl.stenden.spoordock.llmService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolHandlingManager;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingRequestBody;
import nhl.stenden.spoordock.llmService.dtos.EmbeddingResponseBody;
import nhl.stenden.spoordock.llmService.dtos.GenerateRequest;
import nhl.stenden.spoordock.llmService.dtos.GenerateResponse;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.ToolRequest;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.FunctionCall;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.ToolCall;
import nhl.stenden.spoordock.llmService.dtos.ChatRequest;
import nhl.stenden.spoordock.llmService.dtos.ChatResponse;
import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.classes.BotMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.Role;
import nhl.stenden.spoordock.llmService.historyManager.classes.SystemMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.ToolMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage;

@Slf4j
@Component
public class OllamaConnectorService {

    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IChatHistoryManager historyManager;
    private final LlmConfiguration.SystemPrompts systemPrompts;
    private final ToolHandlingManager toolhandlingManager;

    /**
     * Constructs a new OllamaConnectorService with the given configuration and history manager.
     *
     * @param configuration  the LLM configuration containing the base URL and system prompts
     * @param historyManager the chat history manager for storing conversation history
     */
    public OllamaConnectorService(LlmConfiguration configuration, IChatHistoryManager historyManager, ToolHandlingManager toolHandlingManager) {
        String apiUrl = configuration.getBaseUrl();
        this.baseUrl = URI.create(apiUrl).normalize();
        this.historyManager = historyManager;
        this.systemPrompts = configuration.getSystemPrompts();
        this.toolhandlingManager = toolHandlingManager;
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
    
    public void startChatWithToolsStream(UUID chatId, String prompt, String model, Consumer<String> chunkReceived) {

        String systemPrompt = systemPrompts.getDefaultChatPrompt();
        chatStreamWithTools(chatId, prompt, systemPrompt, model, true, 3, chunkReceived);
    }

    private void chatStream(UUID chatId, String prompt, String systemPromp, String model, Consumer<String> chunkReceived) {
        chatStreamWithTools(chatId, prompt, systemPromp, model, false, 1, chunkReceived);
    }

    private void chatStreamWithTools(UUID chatId, String prompt, String systemPrompt, String model, boolean useTools, int maxLoops, Consumer<String> chunkReceived) {
        try{

            systemPrompt += "\n You are allowed to use tools. \n"
                + "When you find it useful to use a tool to answer the user's question, "
                + "Make sure you put those tools in the proper tool field of your response"
                + " and provide the necessary parameters."
                + " If no tools are needed, just provide a normal answer."
                ;

            OllamaMessage systemMessage = new SystemMessage(systemPrompt);
            historyManager.createHistoryIfNotExists(chatId, systemMessage);

            OllamaMessage userMessage = new UserMessage(prompt);
            historyManager.addMessageToHistory(chatId, userMessage);
            
            int loop = 0;
            boolean continueConversation = true;
            while(continueConversation) {

                List<OllamaMessage> messages = historyManager.getHistory(chatId, 20);
                List<ToolRequest> tools = null;
                if (useTools){
                    tools = toolhandlingManager.getAvailableTools();
                }
                
                ChatRequest request = new ChatRequest(model, messages, tools);
                String jsonString = objectMapper.writeValueAsString(request);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(baseUrl.resolve("/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                    .build();
                
                List<ToolCall> toolCalls = new ArrayList<>();
                String fullReceived = "";
                // Stream the response
                HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ChatResponse chunk = objectMapper.readValue(line, ChatResponse.class);
                        if (chunk.getMessage() != null) {

                            if(chunk.getMessage().getContent() != null) {

                                String message = chunk.getMessage().getContent();
                                log.debug("Chunk content: " + message);

                                chunkReceived.accept(chunk.getMessage().getContent());
                                fullReceived += chunk.getMessage().getContent();
                            }

                            if(chunk.getMessage().getToolCalls() != null && !chunk.getMessage().getToolCalls().isEmpty()) {
                                toolCalls.addAll(chunk.getMessage().getToolCalls() );
                            }
                            log.debug("Received chunk: " + line);
                        }
                    }

                    OllamaMessage message = new BotMessage(fullReceived);
                    historyManager.addMessageToHistory(chatId, message);
                }
                catch(Exception ex)
                {
                    throw new RuntimeException("Error reading streamed response", ex);
                }

                //Process tool calls
                for(ToolCall toolCall : toolCalls) {   

                    FunctionCall functionCall = toolCall.getFunctionCall();
                    if(functionCall == null) {
                        continue;
                    }

                    String toolResult = toolhandlingManager.handleToolInvocation(functionCall);
                    OllamaMessage toolMessage = new ToolMessage(toolCall.getFunctionCall().getName(), toolResult);
                    historyManager.addMessageToHistory(chatId, toolMessage);
                }

                loop++;
                if(toolCalls.isEmpty() ||  (maxLoops > 0 && loop >= maxLoops)) {
                    continueConversation = false;
                }

            }
        }catch(Exception ex)
        {
            throw new RuntimeException("Failed to generate text stream", ex);
        }
    }

}
