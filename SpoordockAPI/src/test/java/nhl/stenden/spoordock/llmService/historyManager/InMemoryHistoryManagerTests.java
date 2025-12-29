package nhl.stenden.spoordock.llmService.historyManager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.SystemMessage;
import nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage;

public class InMemoryHistoryManagerTests {


    InMemoryChatHistoryManager historyManager;

    String systemMessage = "You are a helpful assistant.";

    @BeforeEach
    public void setup(){
        historyManager = new InMemoryChatHistoryManager();
    }
    
    @Test
    void testCreateHistoryIfNotExists(){
        
        SystemMessage sysMsg = new SystemMessage(systemMessage);
        
        UUID id = UUID.randomUUID();

        // Act
        historyManager.createHistoryIfNotExists(id, sysMsg);

        // Assert
        List<OllamaMessage> history = historyManager.getHistory(id, 10);
        assert(history.size() == 1);
        assert(history.get(0).getContent().equals(systemMessage));
    }

    @Test
    void testCreateHistory_notCreatedWhenExists(){

        // Arrange
        SystemMessage sysMsg = new SystemMessage(systemMessage);
        UUID id = UUID.randomUUID();
        historyManager.createHistoryIfNotExists(id, sysMsg);

        // Act
        historyManager.createHistoryIfNotExists(id, new SystemMessage("Different system message"));

        // Assert
        List<OllamaMessage> history = historyManager.getHistory(id, 10);
        assert(history.size() == 1);
        assert(history.get(0).getContent().equals(systemMessage));

    }

    @Test
    void testAddMessageToHistory(){
        
        SystemMessage sysMsg = new SystemMessage(systemMessage);
        
        UUID id = UUID.randomUUID();

        // Arrange
        historyManager.createHistoryIfNotExists(id, sysMsg);

        // Act
        var userMsg = new nhl.stenden.spoordock.llmService.historyManager.classes.UserMessage("Hello, world!");
        historyManager.addMessageToHistory(id, userMsg);

        // Assert
        List<OllamaMessage> history = historyManager.getHistory(id, 10);
        assert(history.size() == 2);
        assert(history.get(1).getContent().equals("Hello, world!"));
    }

    @Test
    void addMessageToHistory_invalidUUID_throwsException()  {
        UUID id = UUID.randomUUID();

       assertThrows(IllegalArgumentException.class, () -> {
            historyManager.addMessageToHistory(id, new UserMessage("Hello"));
        });
    }

    @Test
    void testClearHistory_clears(){
    
        SystemMessage sysMsg = new SystemMessage(systemMessage);
        
        UUID id = UUID.randomUUID();

        // Arrange
        historyManager.createHistoryIfNotExists(id, sysMsg);
        historyManager.addMessageToHistory(id, new UserMessage("Hello, world!"));

        // Act
        historyManager.clearHistory(id);

        // Assert
        List<OllamaMessage> history = historyManager.getHistory(id, 10);
        assert(history.size() == 0);
    }

    @Test 
    void getMessageHistoryWithMaxChuncks(){

        SystemMessage sysMsg = new SystemMessage(systemMessage);
        
        UUID id = UUID.randomUUID();

        // Arrange
        historyManager.createHistoryIfNotExists(id, sysMsg);
        historyManager.addMessageToHistory(id, new UserMessage("Message 1"));
        historyManager.addMessageToHistory(id, new UserMessage("Message 2"));
        historyManager.addMessageToHistory(id, new UserMessage("Message 3"));
        historyManager.addMessageToHistory(id, new UserMessage("Message 4"));

        // Act
        List<OllamaMessage> history = historyManager.getHistory(id, 3);

        // Assert
        assert(history.size() == 3);
        assert(history.get(0).getContent().equals(systemMessage)); // System message
        assert(history.get(1).getContent().equals("Message 3"));
        assert(history.get(2).getContent().equals("Message 4"));

    }
    
}
