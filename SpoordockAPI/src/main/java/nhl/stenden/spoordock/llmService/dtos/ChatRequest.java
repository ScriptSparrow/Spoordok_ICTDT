package nhl.stenden.spoordock.llmService.dtos;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest.ToolRequest;
import nhl.stenden.spoordock.llmService.historyManager.classes.OllamaMessage;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String model;
    private List<OllamaMessage> messages;
    private List<ToolRequest> tools;

}
