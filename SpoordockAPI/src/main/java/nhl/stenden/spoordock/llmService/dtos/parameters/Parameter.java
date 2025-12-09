package nhl.stenden.spoordock.llmService.dtos.parameters;

import lombok.Getter;

@Getter
public abstract class Parameter {

    private ParameterType type;

    public Parameter(ParameterType type) {
        this.type = type;
    }

}
