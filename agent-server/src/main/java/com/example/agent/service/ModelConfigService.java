package com.example.agent.service;

import com.example.agent.model.ModelConfig;
import com.example.agent.util.AgentFileUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ModelConfigService {
    private final static Logger logger= LoggerFactory.getLogger(ModelConfigService.class);

    @Value("agent.base.path")
    private String basePath;

    @Value("agent.llm.folder")
    private String llmFolder;

    @Value("agent.llm.file")
    private String llmFile;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ModelConfig> modelConfigs = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadConfigs();
        if (modelConfigs.isEmpty()) {
            // Fallback default if file is empty or missing
            modelConfigs.add(new ModelConfig(
                    UUID.randomUUID().toString(),
                    "ap",
                    "https://open.bigmodel.cn/api/paas/v4",
                    "my_zp_api_key",
                    "GLM-4-Flash"));
            saveConfigs();
        }
    }

    public List<ModelConfig> getAllConfigs() {
        return new ArrayList<>(modelConfigs);
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
        File file = new File(AgentFileUtil.getFilePath(basePath,llmFolder,llmFile));
        if (file.exists()) {
            try {
                List<ModelConfig> loaded = objectMapper.readValue(file, new TypeReference<List<ModelConfig>>() {
                });
                modelConfigs.clear();
                modelConfigs.addAll(loaded);
            } catch (IOException e) {
               logger.error(e.getMessage(),e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(AgentFileUtil.getFilePath(basePath,llmFolder,llmFile)), modelConfigs);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
