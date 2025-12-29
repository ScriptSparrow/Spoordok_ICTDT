package nhl.stenden.spoordock.llmService.historyManager;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;

@Repository
public class InMemoryChatHistoryManager implements IChatHistoryManager {

    private Map<UUID, Queue<OllamaMessage>> conversationHistories = new HashMap<UUID, Queue<OllamaMessage>>();

    @Override
    public void createHistoryIfNotExists(UUID conversationId, OllamaMessage systemMessage) {
        if (!conversationHistories.containsKey(conversationId)) {
            Queue<OllamaMessage> newHistory = new ArrayDeque<>();
            newHistory.add(systemMessage);
            conversationHistories.put(conversationId, newHistory);
        }
    }

    @Override
    public void addMessageToHistory(UUID conversationId, OllamaMessage message) {
        if (!conversationHistories.containsKey(conversationId)) {
            throw new IllegalArgumentException("Conversation ID does not exist: " + conversationId.toString());
        }

        conversationHistories.get(conversationId).add(message);
    }

    @Override
    public void clearHistory(UUID conversationId) {
        conversationHistories.remove(conversationId);
    }

    @Override
    public List<OllamaMessage> getHistory(UUID conversationId, int maxMessages) {
        if (!conversationHistories.containsKey(conversationId)) {
            return new LinkedList<OllamaMessage>();
        }

        Queue<OllamaMessage> fullHistory = conversationHistories.get(conversationId);

        if (fullHistory.size() <= maxMessages) {
            return new LinkedList<>(fullHistory);
        }

        // Always include first message (system) + last (maxMessages - 1) messages
        List<OllamaMessage> result = new LinkedList<>();
        List<OllamaMessage> historyList = new LinkedList<>(fullHistory);

        result.add(historyList.get(0)); // System message

        int skipCount = historyList.size() - (maxMessages - 1);
        historyList.stream()
                .skip(Math.max(1, skipCount)) // Skip from index 1, keep last (maxMessages-1)
                .forEach(result::add);

        return result;
    }

}
