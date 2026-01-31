package com.example.agent.config;

import com.example.agent.model.ModelConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Configuration
public class EmbeddingConfig {
    private final static Logger logger= LoggerFactory.getLogger(EmbeddingConfig.class);

    @Resource
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public EmbeddingModel embeddingModel() {
        ModelConfig modelConfig=getEmbeddingModelConf();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .modelName(modelConfig.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(){
        return new InMemoryEmbeddingStore<>();
    }

    private ModelConfig getEmbeddingModelConf(){
        ModelConfig modelConfig=new ModelConfig();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(agentConfig.getEmbedFilePath()), modelConfig);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
        return modelConfig;
    }
}
