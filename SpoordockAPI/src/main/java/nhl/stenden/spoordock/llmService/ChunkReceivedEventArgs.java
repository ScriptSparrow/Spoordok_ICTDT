package nhl.stenden.spoordock.llmService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class ChunkReceivedEventArgs {


    private String chunk;
    private ChunkType chunkType;

    @Override
    public String toString() {
        return chunkType.toString() + ":" + this.chunk;
    }
}

