package nhl.stenden.spoordock.llmService.historyManager.classes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolMessage extends OllamaMessage {

    @JsonProperty("tool_name")
    private String toolName;

    public ToolMessage(String toolName, String content) {
        super(Role.TOOL, content);
        this.toolName = toolName;
    }
}
