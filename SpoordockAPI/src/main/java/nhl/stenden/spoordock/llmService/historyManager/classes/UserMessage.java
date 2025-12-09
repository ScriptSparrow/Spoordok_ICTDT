package nhl.stenden.spoordock.llmService.historyManager.classes;

import lombok.Getter;

@Getter
public class UserMessage extends OllamaMessage {
    public UserMessage(String content) {
        super(Role.USER, content);
    }
}
