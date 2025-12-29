package nhl.stenden.spoordock.llmService.dtos.parameters;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ObjectParameter extends Parameter {

    private Map<String, Parameter> properties;
    private List<String> required;
    
    public ObjectParameter(Map<String, Parameter> properties) {
        super(ParameterType.OBJECT);

        this.properties = properties;
        required = properties.keySet().stream().toList();
    }

}
