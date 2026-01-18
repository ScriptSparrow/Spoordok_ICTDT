package nhl.stenden.spoordock.llmService.dtos;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.ToolRequest;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter
@NoArgsConstructor
public class ChatRequest {

    public ChatRequest(String model, List<OllamaMessage> messages, List<ToolRequest> tools, Options options) {
        this.model = model;
        this.messages = messages;
        this.tools = tools;
        this.options = options;
    }

    private String model;
    private List<OllamaMessage> messages;
    private List<ToolRequest> tools;
    private boolean stream = true;
    private boolean think = true;
    private Options options;
    
    @JsonProperty("keep_alive")
    private String keepAlive = "5m"; // Keep model loaded for 5 minutes

}
