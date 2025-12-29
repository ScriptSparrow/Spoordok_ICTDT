package nhl.stenden.spoordock.llmService.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class Options {

    @JsonProperty("num_ctx")
    private int contextLenght;

}
