//package com.example.agent.config;
//
//import com.example.agent.model.ModelConfig;
//import com.example.agent.service.ModelConfigService;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.Duration;
//
//@Configuration
//public class EmbeddingConfig {
//
//    private final ModelConfigService modelConfigService;
//
//    public EmbeddingConfig(ModelConfigService modelConfigService) {
//        this.modelConfigService = modelConfigService;
//    }
//
//    @Bean
//    public EmbeddingModel embeddingModel() {
//        var all = modelConfigService.getAllConfigs();
//        ModelConfig config = all==null||all.isEmpty() ? null : all.getFirst();
//        if(config==null){
//            return null;
//        }
//        return OpenAiEmbeddingModel.builder()
//                .baseUrl(config.getBaseUrl())
//                .apiKey(config.getApiKey())
//                .modelName(config.getModelName())
//                .timeout(Duration.ofSeconds(60))
//                .build();
//    }
//}
