package nhl.stenden.spoordock.llmService.dtos.parameters.toolCall;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionCall {
    private String name;
    private String description;
    private Map<String,Object> arguments;
}
