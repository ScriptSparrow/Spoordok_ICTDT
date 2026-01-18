package nhl.stenden.spoordock.llmService.dtos;

import lombok.Setter;
import lombok.Getter;

@Getter 
@Setter
public class EmbeddingRequestBody {
    public String model;
    public String input;
    public int dimensions;

    public EmbeddingRequestBody(String model, String input, int dimensions) {
        this.model = model;
        this.input = input;
        this.dimensions = dimensions;
    }
}