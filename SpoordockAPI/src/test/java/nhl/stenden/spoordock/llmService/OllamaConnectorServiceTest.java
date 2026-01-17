package nhl.stenden.spoordock.llmService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nhl.stenden.spoordock.llmService.ToolHandling.ToolHandlingManager;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.classes.BotMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.SystemMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.ToolMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage;

public class OllamaConnectorServiceTest {

    String baseUrl = "http://someurl.com";
    String defaultModel = "defaultModel";

    private LlmConfiguration llmConfig = new LlmConfiguration(){
        {
            setBaseUrl(baseUrl);
            setDefaultModel(defaultModel);
            setSystemPrompts(new SystemPrompts(){
                {
                    setDefaultChatPrompt("Default Chat prompt");
                    setDescriptionHelperPrompt("Description helper prompt");
                }
            });
        }
    };

    @Mock
    private IChatHistoryManager historyManager;
    
    @Mock
    private ToolHandlingManager toolHandlingManager;
    
    @Mock
    private HttpClient httpClient;
    
    @Mock
    private HttpResponse<InputStream> httpResponse;

    private List<OllamaMessage> history = new ArrayList<OllamaMessage>(){
        {
            add((OllamaMessage)new SystemMessage("Some message"));
            add((OllamaMessage)new UserMessage("First User Message"));
            add((OllamaMessage)new BotMessage("The response to the first message"));
            add((OllamaMessage)new ToolMessage("toolName", "A tool message"));
        }
    };

    private OllamaConnectorService testingService; 

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        when(historyManager.getHistory(any(UUID.class), anyInt()))
            .thenReturn(history);

        String mockResponseBody = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Mock response"},"done":false}
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":" continued"},"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseBody.getBytes());
        
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        when(httpClient.<InputStream>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
            .thenReturn(httpResponse);

        testingService = new OllamaConnectorService(llmConfig, historyManager, toolHandlingManager, httpClient);
    }

    @Test
    void getAvailableModels_returnsListOfModels() throws Exception {
        String mockModelsResponse = """
            {"models":[{"name":"model1"},{"name":"model2"}]}
            """;
        
        HttpResponse<String> modelsHttpResponse = mock(HttpResponse.class);
        when(modelsHttpResponse.statusCode()).thenReturn(200);
        when(modelsHttpResponse.body()).thenReturn(mockModelsResponse);
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(modelsHttpResponse);
        
        List<String> models = testingService.getAvailableModels();
        
        assertNotNull(models);
        assertEquals(2, models.size());
        assertTrue(models.contains("model1"));
        assertTrue(models.contains("model2"));
    }

    @Test
    void getAvailableModels_sendsCorrectHttpRequest() throws Exception {
        String mockModelsResponse = """
            {"models":[{"name":"model1"}]}
            """;
        
        HttpResponse<String> modelsHttpResponse = mock(HttpResponse.class);
        when(modelsHttpResponse.statusCode()).thenReturn(200);
        when(modelsHttpResponse.body()).thenReturn(mockModelsResponse);
        
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        
        when(httpClient.<String>send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(modelsHttpResponse);
        
        testingService.getAvailableModels();
        
        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals("GET", capturedRequest.method());
        assertTrue(capturedRequest.uri().toString().endsWith("/api/tags"));
        assertTrue(capturedRequest.headers().firstValue("Content-Type").orElse("").equals("application/json"));
    }

    @Test
    void getAvailableModels_emptyModelList_returnsEmptyList() throws Exception {
        String mockModelsResponse = """
            {"models":[]}
            """;
        
        HttpResponse<String> modelsHttpResponse = mock(HttpResponse.class);
        when(modelsHttpResponse.statusCode()).thenReturn(200);
        when(modelsHttpResponse.body()).thenReturn(mockModelsResponse);
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(modelsHttpResponse);
        
        List<String> models = testingService.getAvailableModels();
        
        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void getAvailableModels_httpError_throwsException() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(500);
        when(errorResponse.body()).thenReturn("Internal Server Error");
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(errorResponse);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.getAvailableModels();
        });
        
        assertTrue(exception.getMessage().contains("Failed to get available models"));
    }

    @Test
    void getAvailableModels_httpClient404_throwsException() throws Exception {
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(404);
        when(errorResponse.body()).thenReturn("Not Found");
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(errorResponse);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.getAvailableModels();
        });
        
        assertTrue(exception.getMessage().contains("Failed to get available models"));
    }

    @Test
    void getAvailableModels_networkException_throwsRuntimeException() throws Exception {
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenThrow(new java.io.IOException("Network error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.getAvailableModels();
        });
        
        assertTrue(exception.getMessage().contains("Failed to get available models"));
        assertTrue(exception.getCause() instanceof java.io.IOException);
    }

    @Test
    void getAvailableModels_invalidJson_throwsRuntimeException() throws Exception {
        HttpResponse<String> modelsHttpResponse = mock(HttpResponse.class);
        when(modelsHttpResponse.statusCode()).thenReturn(200);
        when(modelsHttpResponse.body()).thenReturn("invalid json");
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(modelsHttpResponse);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.getAvailableModels();
        });
        
        assertTrue(exception.getMessage().contains("Failed to get available models"));
    }

    @Test
    void getAvailableModels_multipleModels_returnsAllModelNames() throws Exception {
        String mockModelsResponse = """
            {"models":[{"name":"llama2"},{"name":"mistral"},{"name":"codellama"}]}
            """;
        
        HttpResponse<String> modelsHttpResponse = mock(HttpResponse.class);
        when(modelsHttpResponse.statusCode()).thenReturn(200);
        when(modelsHttpResponse.body()).thenReturn(mockModelsResponse);
        
        when(httpClient.<String>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(modelsHttpResponse);
        
        List<String> models = testingService.getAvailableModels();
        
        assertEquals(3, models.size());
        assertTrue(models.contains("llama2"));
        assertTrue(models.contains("mistral"));
        assertTrue(models.contains("codellama"));
    }

    @Test
    void createHistory_calledWithNewId() throws Exception {
        UUID id = UUID.randomUUID();

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<OllamaMessage> messageCaptor = ArgumentCaptor.forClass(OllamaMessage.class);
        
        doNothing().when(historyManager).createHistoryIfNotExists(uuidCaptor.capture(), messageCaptor.capture());
            
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "some prompt", defaultModel, chunkConsumer);
        
        assertEquals(id, uuidCaptor.getValue());
        assertTrue(messageCaptor.getValue() instanceof SystemMessage);
        assertEquals("Default Chat prompt", ((SystemMessage)messageCaptor.getValue()).getContent());
    }

    @Test
    void startChatWithToolsStream_addsUserMessageToHistory() throws Exception {
        UUID id = UUID.randomUUID();
        String userPrompt = "test prompt";
        
        ArgumentCaptor<OllamaMessage> messageCaptor = ArgumentCaptor.forClass(OllamaMessage.class);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, userPrompt, defaultModel, chunkConsumer);
        
        verify(historyManager, atLeastOnce()).addMessageToHistory(eq(id), messageCaptor.capture());
        
        List<OllamaMessage> capturedMessages = messageCaptor.getAllValues();
        assertTrue(capturedMessages.stream().anyMatch(msg -> 
            msg instanceof UserMessage && ((UserMessage)msg).getContent().equals(userPrompt)
        ));
    }

    @Test
    void startChatWithToolsStream_sendsHttpRequest() throws Exception {
        UUID id = UUID.randomUUID();
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(httpClient).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
    }

    @Test
    void startChatWithToolsStream_receivesChunks() throws Exception {
        UUID id = UUID.randomUUID();
        List<ChunkReceivedEventArgs> receivedChunks = new ArrayList<>();
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {
            receivedChunks.add(chunk);
        };

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        assertFalse(receivedChunks.isEmpty());
        assertTrue(receivedChunks.stream().anyMatch(chunk -> chunk.getChunkType() == ChunkType.Content));
        assertTrue(receivedChunks.stream().anyMatch(chunk -> chunk.getChunkType() == ChunkType.CompleteChunk));
    }

    @Test
    void generateDescriptionHelperStream_usesDescriptionHelperPrompt() throws Exception {
        UUID id = UUID.randomUUID();
        
        ArgumentCaptor<OllamaMessage> messageCaptor = ArgumentCaptor.forClass(OllamaMessage.class);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.generateDescriptionHelperStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(historyManager).createHistoryIfNotExists(eq(id), messageCaptor.capture());
        
        SystemMessage systemMessage = (SystemMessage) messageCaptor.getValue();
        assertEquals("Description helper prompt", systemMessage.getContent());
    }

    @Test
    void startChatWithToolsStream_httpClientThrowsException_wrapsInRuntimeException() throws Exception {
        UUID id = UUID.randomUUID();
        
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
            .thenThrow(new RuntimeException("Network error"));
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        });
        
        assertTrue(exception.getMessage().contains("Failed to generate text stream"));
    }

    @Test
    void startChatWithToolsStream_addsBotMessageToHistory() throws Exception {
        UUID id = UUID.randomUUID();
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        ArgumentCaptor<OllamaMessage> messageCaptor = ArgumentCaptor.forClass(OllamaMessage.class);
        verify(historyManager, atLeastOnce()).addMessageToHistory(eq(id), messageCaptor.capture());
        
        List<OllamaMessage> capturedMessages = messageCaptor.getAllValues();
        assertTrue(capturedMessages.stream().anyMatch(msg -> msg instanceof BotMessage));
    }

    @Test
    void startChatWithToolsStream_withToolCalls_processesTools() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseWithTools = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Let me check that","tool_calls":[{"function":{"name":"testTool","arguments":{"param":"value"}}}]},"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseWithTools.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        when(toolHandlingManager.getAvailableTools()).thenReturn(List.of());
        when(toolHandlingManager.handleToolInvocation(any())).thenReturn("tool result");
        
        List<ChunkReceivedEventArgs> receivedChunks = new ArrayList<>();
        Consumer<ChunkReceivedEventArgs> chunkConsumer = receivedChunks::add;

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(toolHandlingManager).handleToolInvocation(any());
        assertTrue(receivedChunks.stream().anyMatch(chunk -> chunk.getChunkType() == ChunkType.ToolCall));
    }

    @Test
    void startChatWithToolsStream_withThinkingContent_sendsThinkingChunks() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseWithThinking = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","thinking":"I'm thinking about this"},"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseWithThinking.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        List<ChunkReceivedEventArgs> receivedChunks = new ArrayList<>();
        Consumer<ChunkReceivedEventArgs> chunkConsumer = receivedChunks::add;

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        assertTrue(receivedChunks.stream().anyMatch(chunk -> chunk.getChunkType() == ChunkType.Thinking));
    }

    @Test
    void startChatWithToolsStream_multipleLoopsWithTools_stopsAtMaxLoops() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseWithTools = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Response","tool_calls":[{"function":{"name":"tool1","arguments":{}}}]},"done":true}
            """;
        
        InputStream mockInputStream1 = new ByteArrayInputStream(mockResponseWithTools.getBytes());
        InputStream mockInputStream2 = new ByteArrayInputStream(mockResponseWithTools.getBytes());
        InputStream mockInputStream3 = new ByteArrayInputStream(mockResponseWithTools.getBytes());
        
        when(httpResponse.body())
            .thenReturn(mockInputStream1)
            .thenReturn(mockInputStream2)
            .thenReturn(mockInputStream3);
        
        when(toolHandlingManager.getAvailableTools()).thenReturn(List.of());
        when(toolHandlingManager.handleToolInvocation(any())).thenReturn("result");
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(httpClient, times(3)).send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
    }

    @Test
    void generateDescriptionHelperStream_doesNotUseTools() throws Exception {
        UUID id = UUID.randomUUID();
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.generateDescriptionHelperStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(toolHandlingManager, never()).getAvailableTools();
        verify(toolHandlingManager, never()).handleToolInvocation(any());
    }

    @Test
    void chatStreamWithTools_nullToolCall_skipsProcessing() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseWithNullToolCall = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Response","tool_calls":[{"function":null}]},"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseWithNullToolCall.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        when(toolHandlingManager.getAvailableTools()).thenReturn(List.of());
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(toolHandlingManager, never()).handleToolInvocation(any());
    }

    @Test
    void chatStreamWithTools_emptyMessage_handlesGracefully() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseEmpty = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":null,"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseEmpty.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        assertDoesNotThrow(() -> {
            testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        });
    }

    @Test
    void chatStreamWithTools_invalidJsonInStream_throwsException() throws Exception {
        UUID id = UUID.randomUUID();
        
        String invalidJson = "not valid json";
        InputStream mockInputStream = new ByteArrayInputStream(invalidJson.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        });
        
        assertTrue(exception.getMessage().contains("Failed to generate text stream") || 
                   exception.getCause().getMessage().contains("Error reading streamed response"));
    }

    @Test
    void startChatWithToolsStream_retrievesHistoryWithCorrectLimit() throws Exception {
        UUID id = UUID.randomUUID();
        
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(historyManager, atLeastOnce()).getHistory(eq(id), limitCaptor.capture());
        assertEquals(20, limitCaptor.getValue());
    }

    @Test
    void startChatWithToolsStream_sendsCorrectUriAndHeaders() throws Exception {
        UUID id = UUID.randomUUID();
        
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
        
        HttpRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.uri().toString().endsWith("/api/chat"));
        assertTrue(capturedRequest.headers().firstValue("Content-Type").orElse("").equals("application/json"));
    }

    @Test
    void startChatWithToolsStream_addsToolMessageToHistory() throws Exception {
        UUID id = UUID.randomUUID();
        
        String mockResponseWithTools = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Using tool","tool_calls":[{"function":{"name":"myTool","arguments":{}}}]},"done":true}
            """;
        
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseWithTools.getBytes());
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        when(toolHandlingManager.getAvailableTools()).thenReturn(List.of());
        when(toolHandlingManager.handleToolInvocation(any())).thenReturn("tool result");
        
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {};

        testingService.startChatWithToolsStream(id, "test prompt", defaultModel, chunkConsumer);
        
        ArgumentCaptor<OllamaMessage> messageCaptor = ArgumentCaptor.forClass(OllamaMessage.class);
        verify(historyManager, atLeastOnce()).addMessageToHistory(eq(id), messageCaptor.capture());
        
        List<OllamaMessage> capturedMessages = messageCaptor.getAllValues();
        assertTrue(capturedMessages.stream().anyMatch(msg -> msg instanceof ToolMessage));
    }
}
