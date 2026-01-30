package com.example.agent.config;

import com.example.agent.model.ModelConfig;
import com.example.agent.service.ModelConfigService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class EmbeddingConfig {

    private final ModelConfigService modelConfigService;

    public EmbeddingConfig(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        var all = modelConfigService.getAllConfigs();
        ModelConfig config = all.isEmpty() ? null : all.get(0);

        String baseUrl = (config != null && config.getBaseUrl() != null && !config.getBaseUrl().isEmpty())
                ? config.getBaseUrl()
                : "https://api.openai.com/v1";
        String apiKey = (config != null && config.getApiKey() != null && !config.getApiKey().isEmpty())
                ? config.getApiKey()
                : "demo";

        // LangChain4j OpenAiEmbeddingModel
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("text-embedding-3-small")
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
