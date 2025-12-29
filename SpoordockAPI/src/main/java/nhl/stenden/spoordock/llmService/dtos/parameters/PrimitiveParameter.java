package nhl.stenden.spoordock.llmService.dtos.parameters;

import lombok.Getter;

@Getter
public class PrimitiveParameter extends Parameter {
    
    private String description;
    public PrimitiveParameter(ParameterType type, String description) {
        super(type);
         this.description = description;
    }
}
