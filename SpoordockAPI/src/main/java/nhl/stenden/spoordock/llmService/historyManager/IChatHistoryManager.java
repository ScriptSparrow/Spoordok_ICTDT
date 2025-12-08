package nhl.stenden.spoordock.llmService.historyManager;

import java.util.List;
import java.util.UUID;

import nhl.stenden.spoordock.llmService.historyManager.classes.HistoricalMessage;

public interface IChatHistoryManager {
    public void createHistoryIfNotExists(UUID conversationId, HistoricalMessage systemMessage);
    public void addMessageToHistory(UUID conversationId, HistoricalMessage message);

    public void clearHistory(UUID conversationId);
    public List<HistoricalMessage> getHistory(UUID conversationId, int maxMessages);

    
}
