package nhl.stenden.spoordock.llmService.dtos;

import lombok.Setter;
import lombok.Getter;

@Getter 
@Setter
public class EmbeddingRequestBody {
    public String model;
    public String input;

    public EmbeddingRequestBody(String model, String input) {
        this.model = model;
        this.input = input;
    }
}