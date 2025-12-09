package nhl.stenden.spoordock.llmService.dtos.parameters;

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

    @Override
    public String toString() {
        return this.typeName;
    }

}
