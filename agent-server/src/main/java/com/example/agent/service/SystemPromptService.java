package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.SystemPrompt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SystemPromptService extends BaseService {
    private final static Logger logger = LoggerFactory.getLogger(SystemPromptService.class);

    @Autowired
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<SystemPrompt> prompts = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    public List<SystemPrompt> getAllPrompts() {
        return new ArrayList<>(prompts);
    }

    // 获取当前激活的系统提示词
    public SystemPrompt getActivePrompt() {
        return prompts.stream().filter(SystemPrompt::isActive).findFirst().orElse(null);
    }

    public SystemPrompt addPrompt(SystemPrompt prompt) {
        if (prompt.getId() == null || prompt.getId().isEmpty()) {
            prompt.setId(UUID.randomUUID().toString());
        }
        // 如果是第一个添加的，默认激活；或者如果新添加的设为激活，需取消其他的
        if (prompts.isEmpty()) {
            prompt.setActive(true);
        } else if (prompt.isActive()) {
            deactivateAll();
        }
        prompts.add(prompt);
        saveConfigs();
        return prompt;
    }

    public void updatePrompt(SystemPrompt prompt) {
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId().equals(prompt.getId())) {
                // 如果更新为激活状态，先取消其他所有激活状态
                if (prompt.isActive() && !prompts.get(i).isActive()) {
                    deactivateAll();
                }
                prompts.set(i, prompt);
                saveConfigs();
                return;
            }
        }
    }

    public void deletePrompt(String id) {
        prompts.removeIf(p -> p.getId().equals(id));
        saveConfigs();
    }

    // 激活指定ID，禁用其他
    public void activatePrompt(String id) {
        for (SystemPrompt p : prompts) {
            p.setActive(p.getId().equals(id));
        }
        saveConfigs();
    }

    private void deactivateAll() {
        for (SystemPrompt p : prompts) {
            p.setActive(false);
        }
    }

    private void loadConfigs() {
        File file = new File(agentConfig.getSystemPromptFilePath());
        if (file.exists()) {
            try {
                List<SystemPrompt> loaded = objectMapper.readValue(file, new TypeReference<List<SystemPrompt>>() {});
                prompts.clear();
                prompts.addAll(loaded);
            } catch (IOException e) {
                logger.error("Failed to load System Prompts", e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            File file = new File(agentConfig.getSystemPromptFilePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, prompts);
        } catch (IOException e) {
            logger.error("Failed to save System Prompts", e);
        }
    }
}
