package nhl.stenden.spoordock.llmService.historyManager.classes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BotMessage extends OllamaMessage {
    public BotMessage(String content) {
        super(Role.ASSISTANT, content);
    }
}
