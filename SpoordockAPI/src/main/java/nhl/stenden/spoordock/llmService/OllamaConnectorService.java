package nhl.stenden.spoordock.llmService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import nhl.stenden.spoordock.llmService.ToolHandling.ToolHandlingManager;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration.ModelConfig;
import nhl.stenden.spoordock.llmService.dtos.Options;
import nhl.stenden.spoordock.llmService.dtos.ChatResponse.Message;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.ToolRequest;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.FunctionCall;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.ToolCall;
import nhl.stenden.spoordock.llmService.dtos.ChatRequest;
import nhl.stenden.spoordock.llmService.dtos.ChatResponse;
import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.classes.BotMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.SystemMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.ToolMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage;

@Slf4j
@Component
public class OllamaConnectorService {

    
    
    private final HttpClient httpClient;
    private final URI baseUrl;
    private final Map<String, LlmConfiguration.ModelConfig> modelConfigs = new HashMap<>();
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
    public OllamaConnectorService(LlmConfiguration configuration, IChatHistoryManager historyManager, ToolHandlingManager toolHandlingManager, HttpClient httpClient) {
        String apiUrl = configuration.getBaseUrl();
        this.baseUrl = URI.create(apiUrl).normalize();
        this.historyManager = historyManager;
        this.systemPrompts = configuration.getSystemPrompts();
        this.toolhandlingManager = toolHandlingManager;
        this.httpClient = httpClient;

        for (LlmConfiguration.ModelConfig modelConfig : configuration.getModels()) {
            modelConfigs.put(modelConfig.getName(), modelConfig);
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
    public void generateDescriptionHelperStream(UUID chatId, String prompt, String model, Consumer<ChunkReceivedEventArgs> chunkReceived) {
        chatStream(chatId, prompt, systemPrompts.getDescriptionHelperPrompt(), model, chunkReceived);
    }
    
    public void startChatWithToolsStream(UUID chatId, String prompt, String model, Consumer<ChunkReceivedEventArgs> chunkReceived) {

        String systemPrompt = systemPrompts.getDefaultChatPrompt();
        chatStreamWithTools(chatId, prompt, systemPrompt, model, true, 3, chunkReceived);
    }

    private void chatStream(UUID chatId, String prompt, String systemPromp, String model, Consumer<ChunkReceivedEventArgs> chunkReceived) {
        chatStreamWithTools(chatId, prompt, systemPromp, model, false, 1, chunkReceived);
    }

    private void chatStreamWithTools(UUID chatId, 
            String prompt, 
            String systemPrompt, 
            String model, 
            boolean useTools, 
            int maxLoops, 
            Consumer<ChunkReceivedEventArgs> chunkReceived)  {
        try{

            OllamaMessage systemMessage = new SystemMessage(systemPrompt);
            historyManager.createHistoryIfNotExists(chatId, systemMessage);

            OllamaMessage userMessage = new UserMessage(prompt);
            historyManager.addMessageToHistory(chatId, userMessage);
            
            List<ToolRequest> tools = null;
            if (useTools){
                tools = toolhandlingManager.getAvailableTools();
            }

            ModelConfig modelConfig = modelConfigs.get(model);
            if (modelConfig == null) {
                throw new IllegalArgumentException("Model '" + model + "' is not configured.");
            }

            Options options = new Options(modelConfig.getContextLength());

            int loop = 0;
            boolean continueConversation = true;
            while(continueConversation) {

                List<OllamaMessage> messages = historyManager.getHistory(chatId, 20);
                ChatRequest request = new ChatRequest(model, messages, tools, options);
                log.debug(objectMapper.writeValueAsString(request));
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
                        Message message = chunk.getMessage();
                        if (message != null) {

                            if(message.getContent() != null) {
                                String content = message.getContent();

                                ChunkReceivedEventArgs args = new ChunkReceivedEventArgs(content, ChunkType.Content);
                                chunkReceived.accept(args);
                                fullReceived += content;
                            }

                            if(message.getThinking() != null) {
                                ChunkReceivedEventArgs args = new ChunkReceivedEventArgs(message.getThinking(), ChunkType.Thinking);
                               chunkReceived.accept(args);
                            }

                            if(message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                                toolCalls.addAll(message.getToolCalls() );
                            }
                        }
                    }

                    log.debug("Responded with \n tool_calls: " + toolCalls.size() + "\nmessage: " + fullReceived);

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
                    String toolCallData = String.format("{ \"tool_call\" : \"%s\" , \"raw_result\": \"%s\"}", functionCall.getName(), toolResult);
                    ChunkReceivedEventArgs args = new ChunkReceivedEventArgs(toolCallData, ChunkType.ToolCall);
                    chunkReceived.accept(args);

                    OllamaMessage toolMessage = new ToolMessage(toolCall.getFunctionCall().getName(), toolResult);
                    historyManager.addMessageToHistory(chatId, toolMessage);
                }

                loop++;
                if(toolCalls.isEmpty() ||  (maxLoops > 0 && loop >= maxLoops)) {
                    continueConversation = false;
                }

            }

            ChunkReceivedEventArgs args = new ChunkReceivedEventArgs("", ChunkType.CompleteChunk);
            chunkReceived.accept(args);
        }catch(Exception ex)
        {
            throw new RuntimeException("Failed to generate text stream", ex);
        }
    }

}
