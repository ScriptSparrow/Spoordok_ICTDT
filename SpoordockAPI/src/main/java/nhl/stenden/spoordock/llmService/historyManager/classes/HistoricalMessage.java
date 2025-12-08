package nhl.stenden.spoordock.llmService.historyManager.classes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalMessage {

    public Role role;
    public String content;

    public HistoricalMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

}
