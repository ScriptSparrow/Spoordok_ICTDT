package nhl.stenden.spoordock.llmService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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


    private IChatHistoryManager historyManager = mock(IChatHistoryManager.class);
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
    void beforeAll(){

        
        when(historyManager.getHistory(any(UUID.class), anyInt()))
            .thenReturn(history);

        testingService = new OllamaConnectorService(llmConfig, historyManager, null);
    }

    @Test
    void createHistory_calledWithNewId(){

        UUID id = UUID.randomUUID();

        doAnswer(invocation -> {
            UUID received = invocation.getArgument(0);
            assertEquals(id, received);
            return null;
        }).when(historyManager).createHistoryIfNotExists(any(UUID.class), any(OllamaMessage.class));
            

        testingService.startChatWithToolsStream(id, "some prompt", defaultModel, null);
    }
    


}
