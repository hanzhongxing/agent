package com.example.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MemoryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private static final String MEMORY_FILE_PATH = "src/main/resources/memory.json";
    private static final int MAX_MESSAGES = 50;

    @PostConstruct
    public void init() {
        loadMemory();
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        saveMemory();
    }

    public void clear() {
        messages.clear();
        saveMemory();
    }

    private void loadMemory() {
        File file = new File(MEMORY_FILE_PATH);
        if (file.exists()) {
            try {
                List<Map<String, String>> data = objectMapper.readValue(file,
                        new TypeReference<List<Map<String, String>>>() {
                        });
                messages.clear();
                for (Map<String, String> entry : data) {
                    String type = entry.get("type");
                    String content = entry.get("content");
                    if ("user".equals(type)) {
                        messages.add(new UserMessage(content));
                    } else if ("assistant".equals(type)) {
                        messages.add(new AiMessage(content));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void saveMemory() {
        try {
            List<Map<String, String>> data = new ArrayList<>();
            for (ChatMessage msg : messages) {
                if (msg instanceof UserMessage) {
                    data.add(Map.of("type", "user", "content", ((UserMessage) msg).singleText()));
                } else if (msg instanceof AiMessage) {
                    data.add(Map.of("type", "assistant", "content", ((AiMessage) msg).text()));
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(MEMORY_FILE_PATH), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
