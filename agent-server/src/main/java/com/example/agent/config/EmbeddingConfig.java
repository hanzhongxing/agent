//package com.example.agent.config;
//
//import com.example.agent.model.ModelConfig;
//import com.example.agent.service.ModelConfigService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
//import jakarta.annotation.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import java.time.Duration;
//
//@Configuration
//public class EmbeddingConfig {
//    private final static Logger logger= LoggerFactory.getLogger(EmbeddingConfig.class);
//
//    @Resource
//    private AgentConfig agentConfig;
//
//    @Autowired
//    private ModelConfigService modelConfigService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public EmbeddingModel getEmbeddingModel() {
//        ModelConfig modelConfig=modelConfigService.getEmbedModelConf();
//        if(modelConfig==null){
//            return null;
//        }
//        return OpenAiEmbeddingModel.builder()
//                .baseUrl(modelConfig.getBaseUrl())
//                .apiKey(modelConfig.getApiKey())
//                .modelName(modelConfig.getModelName())
//                .timeout(Duration.ofSeconds(60))
//                .build();
//    }
//}
