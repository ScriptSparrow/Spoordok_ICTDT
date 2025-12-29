package nhl.stenden.spoordock.llmService.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nhl.stenden.spoordock.llmService.dtos.parameters.toolCall.ToolCall;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {

    private String model;
    private Message message;    
    
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
        private String thinking;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls; 
    }

}
