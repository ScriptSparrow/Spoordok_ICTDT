package nhl.stenden.spoordock.llmService;

import org.junit.jupiter.api.Test;

public class ChunkTypeTests {

    @Test
    public void chunkType_ToString_ReturnsExpectedString() {
        assert(ChunkType.Content.toString().equals("content"));
        assert(ChunkType.ToolCall.toString().equals("tool_call"));
        assert(ChunkType.Thinking.toString().equals("thinking"));
        assert(ChunkType.FullMessage.toString().equals("full_message"));
        assert(ChunkType.CompleteChunk.toString().equals("complete_chunk"));
    }

}
