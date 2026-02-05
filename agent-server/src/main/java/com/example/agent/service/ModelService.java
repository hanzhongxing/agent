package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.ModelInfo;
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
public class ModelService extends BaseService{
    private final static Logger logger= LoggerFactory.getLogger(ModelService.class);

    @Resource
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ModelInfo> modelInfos = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    public List<ModelInfo> getAllConfigs() {
       return new ArrayList<>(modelInfos);
    }

    public List<ModelInfo> getChatConfigs(){
        return modelInfos.stream().filter(modelInfo -> !modelInfo.isEmbed()).collect(Collectors.toList());
    }

    public EmbeddingModel getEmbeddingModel() {
        ModelInfo modelInfo =getEmbedModelConf();
        if(modelInfo ==null){
            return null;
        }
        return OpenAiEmbeddingModel.builder()
                .baseUrl(modelInfo.getBaseUrl())
                .apiKey(modelInfo.getApiKey())
                .modelName(modelInfo.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private ModelInfo getEmbedModelConf(){
        return modelInfos.stream().filter(ModelInfo::isEmbed).toList().getFirst();
    }

    public ModelInfo getConfig(String id) {
        return modelInfos.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ModelInfo addConfig(ModelInfo config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        modelInfos.add(config);
        saveConfigs();
        return config;
    }

    public void updateConfig(ModelInfo config) {
        for (int i = 0; i < modelInfos.size(); i++) {
            if (modelInfos.get(i).getId().equals(config.getId())) {
                modelInfos.set(i, config);
                saveConfigs();
                return;
            }
        }
    }

    public void deleteConfig(String id) {
        modelInfos.removeIf(c -> c.getId().equals(id));
        saveConfigs();
    }

    private void loadConfigs() {
        File file = new File(agentConfig.getLlmFilePath());
        if (file.exists()) {
            try {
                List<ModelInfo> loaded = objectMapper.readValue(file, new TypeReference<List<ModelInfo>>() {});
                modelInfos.clear();
                modelInfos.addAll(loaded);
            } catch (IOException e) {
               logger.error(e.getMessage(),e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(agentConfig.getLlmFilePath()), modelInfos);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
