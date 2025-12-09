package nhl.stenden.spoordock.llmService.historyManager.classes;

public class SystemMessage extends OllamaMessage {

    public SystemMessage(String content) {
        super(Role.SYSTEM, content);
    }

}
