package nhl.stenden.spoordock.llmService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import nhl.stenden.spoordock.llmService.ToolHandling.ToolHandlingManager;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration.ModelConfig;
import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.classes.BotMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.SystemMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.ToolMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage;

public class OllamaConnectorServiceTest {

    String baseUrl = "http://someurl.com";
    String defaultModel = "defaultModel";
    int defaultModelContextLenght = 4000;
    String overrideModel = "overrideModel";
    int overrideModelContextLength;

    List<ModelConfig> config = new ArrayList<ModelConfig>(){
        {
            add(new ModelConfig(){{ setName(defaultModel); setContextLength(defaultModelContextLenght);} });
            add(new ModelConfig(){{ setName(overrideModel); setContextLength(overrideModelContextLength); }});
        }
    };

    private LlmConfiguration llmConfig = new LlmConfiguration(){
        {
            setBaseUrl(baseUrl);
            setDefaultModel(defaultModel);
            setModels(config);
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

        // Mock HTTP streaming response - simulate the JSON chunks that Ollama returns
        String mockResponseBody = """
            {"model":"defaultModel","created_at":"2023-12-15T10:00:00Z","message":{"role":"assistant","content":"Mock response"},"done":true}
            """;
        
        // Create an InputStream from the mock response
        InputStream mockInputStream = new ByteArrayInputStream(mockResponseBody.getBytes());
        
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockInputStream);
        
        // Mock HttpClient to return our mock response for InputStream
        when(httpClient.<InputStream>send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
            .thenReturn(httpResponse);

        testingService = new OllamaConnectorService(llmConfig, historyManager, toolHandlingManager, httpClient);
    }

    @Test
    void createHistory_calledWithNewId(){
        UUID id = UUID.randomUUID();

        doAnswer(invocation -> {
            UUID received = invocation.getArgument(0);
            assertEquals(id, received);
            return null;
        }).when(historyManager).createHistoryIfNotExists(any(UUID.class), any(OllamaMessage.class));
            
        Consumer<ChunkReceivedEventArgs> chunkConsumer = (chunk) -> {
            
        };

        testingService.startChatWithToolsStream(id, "some prompt", defaultModel, chunkConsumer);
    }

    @Test
    void WhenMessageReturnedNull_NothingIsProcessed() {



        
    }

    
}
