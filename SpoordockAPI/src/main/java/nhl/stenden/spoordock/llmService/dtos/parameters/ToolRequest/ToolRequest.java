package nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class ToolRequest {

    public ToolRequest(Function function) {
        this.function = function;
    }

    // Always "function" for LLM function calling
    private String type = "function";
    private Function function;
}
