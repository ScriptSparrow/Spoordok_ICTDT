package nhl.stenden.spoordock.llmService.dtos.parameters.ToolRequest;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nhl.stenden.spoordock.llmService.dtos.parameters.ObjectParameter;
import nhl.stenden.spoordock.llmService.dtos.parameters.Parameter;

@Getter @Setter
@AllArgsConstructor
public class Function {
    private String name;
    private String description;
    private ObjectParameter parameters;
}