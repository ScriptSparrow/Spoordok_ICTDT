package nhl.stenden.spoordock.llmService.dtos.parameters;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ParameterType {

    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    OBJECT("object");

    private String typeName;
    private ParameterType(String typeName) {
        this.typeName = typeName;
    }

    @JsonValue
    public String toString() {
        return this.typeName.toLowerCase();
    }

}
