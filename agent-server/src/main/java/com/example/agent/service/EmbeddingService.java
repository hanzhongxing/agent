package com.example.agent.service;

import com.example.agent.model.ModelConfig;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    @Autowired
    private ModelConfigService modelConfigService;

    public List<EmbeddingModel> getEmbeddingModels(){
        List<EmbeddingModel> models=new ArrayList<>();
        var all = modelConfigService.getAllConfigs();
        if(all==null||all.isEmpty()){
            return models;
        }
        for(ModelConfig config:all) {
            models.add(OpenAiEmbeddingModel.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .timeout(Duration.ofSeconds(60))
                    .build());
        }
        return models;
    }
}
