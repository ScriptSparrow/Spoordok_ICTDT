package nhl.stenden.spoordock.llmService.historyManager;

import java.util.List;
import java.util.UUID;

import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;

public interface IChatHistoryManager {
    public void createHistoryIfNotExists(UUID conversationId, OllamaMessage systemMessage);
    public void addMessageToHistory(UUID conversationId, OllamaMessage message);

    public void clearHistory(UUID conversationId);
    public List<OllamaMessage> getHistory(UUID conversationId, int maxMessages);

    
}
