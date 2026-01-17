package nhl.stenden.spoordock.controllers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Parameter;
import nhl.stenden.spoordock.controllers.dtos.ai.ChatRequest;
import nhl.stenden.spoordock.llmService.configuration.LlmConfiguration;
import nhl.stenden.spoordock.llmService.ChunkReceivedEventArgs;
import nhl.stenden.spoordock.llmService.OllamaConnectorService;

@RestController
@RequestMapping("/api/ai")
public class AiAgentController {

    private final LlmConfiguration llmConfiguration;
    private final OllamaConnectorService ollamaConnectorService;
    private final ObjectMapper  objectMapper = new ObjectMapper();

    public AiAgentController(LlmConfiguration llmConfiguration, OllamaConnectorService ollamaConnectorService) {
        this.llmConfiguration = llmConfiguration;
        this.ollamaConnectorService = ollamaConnectorService;
    }

    @GetMapping("models")
    public ResponseEntity<?> getAvailableModels() {
        try{
            var availableModels = ollamaConnectorService.getAvailableModels();
             return ResponseEntity.ok(
                Map.of(
                    "availableModels", availableModels,
                    "defaultModel", llmConfiguration.getDefaultModel()
                )
            );
        } catch(Exception ex)
        {
            return ResponseEntity
                .internalServerError()
                .body("Failed to retrieve available models: " + ex.getMessage());
        }

       
    }

    @PostMapping("chat/{id}")
    public ResponseEntity<?> chat(
        @Parameter (description = "The unique identifier for the chat session", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable("id") UUID id,
        @RequestBody ChatRequest chatRequest,
        @RequestHeader(value = "model", required = false) String model
    ) {
        
        // Use a new final variable immediately
        final String selectedModel = (model == null || model.isEmpty()) 
            ? llmConfiguration.getDefaultModel() 
            : model;

        var availableModels = ollamaConnectorService.getAvailableModels();
        if (!availableModels.stream().anyMatch(m -> m.equals(selectedModel))) {
            return ResponseEntity
                .badRequest()
                .body("Model '" + selectedModel + "' is not available. Available models: " + availableModels);
        }

        final String message = chatRequest.getMessage();
        if (message == null || message.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body("Message cannot be empty");
        }

        final SseEmitter emitter = new SseEmitter(0L); // No timeout, LLMs aren't that fast 
        CompletableFuture.runAsync(() -> streamResponse(emitter, id, selectedModel, message));
        return ResponseEntity.ok(emitter);
    }

    
    private void streamResponse(SseEmitter emitter, UUID id, String model, String message) {
        try {
            ollamaConnectorService.startChatWithToolsStream(id, message, model, text -> sendChunkEvent(emitter, text));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendChunkEvent(SseEmitter emitter, ChunkReceivedEventArgs args) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(args)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }


    /*SSE EVENT javascript code
        async function streamChat(id, message, model) {
            const response = await fetch(`/api/ai/chat/${id}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'model': model || ''
                },
                body: JSON.stringify({ message })
            });

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                
                // Parse SSE format: "data: ...\n\n"
                const lines = chunk.split('\n');
                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const text = line.slice(5).trim();
                        console.log('Received:', text);
                        // Append to your UI here
                        document.getElementById('output').textContent += text;
                    }
                }
            }
        }
    */

    @PostMapping("description/{id}")
    public ResponseEntity<StreamingResponseBody> helpWithDescription() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
