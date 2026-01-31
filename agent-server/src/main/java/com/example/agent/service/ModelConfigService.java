package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.ModelConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class ModelConfigService {
    private final static Logger logger= LoggerFactory.getLogger(ModelConfigService.class);

    @Resource
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ModelConfig> modelConfigs = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    public List<ModelConfig> getAllConfigs() {
       return new ArrayList<>(modelConfigs);
    }

    public List<ModelConfig> getChatConfigs(){
        return modelConfigs.stream().filter(modelConfig -> modelConfig.getEmbed()==null||!modelConfig.getEmbed()).collect(Collectors.toList());
    }

    public EmbeddingModel getEmbeddingModel() {
        ModelConfig modelConfig=getEmbedModelConf();
        if(modelConfig==null){
            return null;
        }
        return OpenAiEmbeddingModel.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .modelName(modelConfig.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private ModelConfig getEmbedModelConf(){
        for(ModelConfig modelConfig:modelConfigs){
            if(modelConfig.getEmbed()!=null&&modelConfig.getEmbed()){
                return modelConfig;
            }
        }
        return null;
    }

    public ModelConfig getConfig(String id) {
        return modelConfigs.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ModelConfig addConfig(ModelConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        modelConfigs.add(config);
        saveConfigs();
        return config;
    }

    public void updateConfig(ModelConfig config) {
        for (int i = 0; i < modelConfigs.size(); i++) {
            if (modelConfigs.get(i).getId().equals(config.getId())) {
                modelConfigs.set(i, config);
                saveConfigs();
                return;
            }
        }
    }

    public void deleteConfig(String id) {
        modelConfigs.removeIf(c -> c.getId().equals(id));
        saveConfigs();
    }

    private void loadConfigs() {
        File file = new File(agentConfig.getLlmFilePath());
        if (file.exists()) {
            try {
                List<ModelConfig> loaded = objectMapper.readValue(file, new TypeReference<List<ModelConfig>>() {});
                modelConfigs.clear();
                modelConfigs.addAll(loaded);
            } catch (IOException e) {
               logger.error(e.getMessage(),e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(agentConfig.getLlmFilePath()), modelConfigs);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
