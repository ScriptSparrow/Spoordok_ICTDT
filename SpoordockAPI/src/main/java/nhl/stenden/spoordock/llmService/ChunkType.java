package nhl.stenden.spoordock.llmService;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChunkType {
        
        Content("content"),
        ToolCall("tool_call"),
        Thinking("thinking"),
        FullMessage("full_message"),
        CompleteChunk("complete_chunk");

        private String chunkType;
        private ChunkType(String chunkType) {
            this.chunkType = chunkType;
        } 

        @Override
        @JsonValue
        public String toString() {
            return chunkType;
        }
}
