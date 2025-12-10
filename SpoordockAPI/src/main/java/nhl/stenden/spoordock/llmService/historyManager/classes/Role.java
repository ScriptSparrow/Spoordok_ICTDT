package nhl.stenden.spoordock.llmService.historyManager.classes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private String _role;
    private Role(String string){
        this._role = string;
    }

    @JsonValue 
    public String toString() {
        return this._role;
    }
}
