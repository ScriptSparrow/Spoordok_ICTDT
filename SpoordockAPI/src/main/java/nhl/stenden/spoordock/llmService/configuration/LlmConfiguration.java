package nhl.stenden.spoordock.llmService.configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
public class LlmConfiguration {

    private String baseUrl;
    private String defaultModel;
    private SystemPrompts systemPrompts;

    @Getter @Setter
    public static class SystemPrompts {
        private String descriptionHelperPrompt;
        private String defaultChatPrompt;
        private String analysisPrompt;

    }
}
