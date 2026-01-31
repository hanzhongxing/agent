package com.example.agent.service;

import com.example.agent.model.ChatSession;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MemoryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, List<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChatSession> sessionMetadata = new ConcurrentHashMap<>();
    private static final String MEMORY_FILE_PATH = "src/main/resources/memory.json";
    private static final int MAX_MESSAGES = 50;

    @PostConstruct
    public void init() {
        loadMemory();
    }

    public List<ChatMessage> getMessages(String sessionId) {
        return new ArrayList<>(sessionMessages.getOrDefault(sessionId, new ArrayList<>()));
    }

    public void addMessage(String sessionId, ChatMessage message) {
        List<ChatMessage> messages = sessionMessages.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        // If metadata doesn't exist, create default
        if (!sessionMetadata.containsKey(sessionId)) {
            sessionMetadata.put(sessionId, ChatSession.builder()
                    .id(sessionId)
                    .title("Conversation " + sessionId)
                    .useMemory(true)
                    .useRag(false)
                    .build());
        }
        saveMemory();
    }

    public void clear(String sessionId) {
        sessionMessages.remove(sessionId);
        sessionMetadata.remove(sessionId); // Also remove metadata when clearing
        saveMemory();
    }

    public List<ChatSession> getSessions() {
        return new ArrayList<>(sessionMetadata.values());
    }

    public void updateSession(ChatSession session) {
        if (session.getId() != null) {
            sessionMetadata.put(session.getId(), session);
            saveMemory();
        }
    }

    public void deleteSession(String sessionId) {
        sessionMessages.remove(sessionId);
        sessionMetadata.remove(sessionId);
        saveMemory();
    }

    private void loadMemory() {
        File file = new File(MEMORY_FILE_PATH);
        if (file.exists()) {
            try {
                Map<String, Object> root = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
                });

                // Load Metadata
                if (root.get("metadata") instanceof Map) {
                    Map<String, Object> metaMap = (Map<String, Object>) root.get("metadata");
                    for (Map.Entry<String, Object> entry : metaMap.entrySet()) {
                        try {
                            ChatSession session = objectMapper.convertValue(entry.getValue(), ChatSession.class);
                            sessionMetadata.put(entry.getKey(), session);
                        } catch (Exception e) {
                            System.err.println(
                                    "Failed to load metadata for session " + entry.getKey() + ": " + e.getMessage());
                        }
                    }
                }

                // Load Messages
                if (root.get("messages") instanceof Map) {
                    Map<String, Object> msgsMap = (Map<String, Object>) root.get("messages");
                    for (Map.Entry<String, Object> sessionEntry : msgsMap.entrySet()) {
                        try {
                            List<Map<String, String>> msgList = objectMapper.convertValue(sessionEntry.getValue(),
                                    new TypeReference<List<Map<String, String>>>() {
                                    });
                            List<ChatMessage> messages = new CopyOnWriteArrayList<>();
                            for (Map<String, String> entry : msgList) {
                                String type = entry.get("type");
                                String content = entry.get("content");
                                if ("user".equals(type)) {
                                    messages.add(new UserMessage(content));
                                } else if ("assistant".equals(type)) {
                                    messages.add(new AiMessage(content));
                                }
                            }
                            sessionMessages.put(sessionEntry.getKey(), messages);
                        } catch (Exception e) {
                            System.err.println("Failed to load messages for session " + sessionEntry.getKey() + ": "
                                    + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void saveMemory() {
        try {
            Map<String, Object> root = new java.util.HashMap<>();

            // Save Metadata
            root.put("metadata", sessionMetadata);

            // Save Messages
            Map<String, List<Map<String, String>>> msgsData = new java.util.HashMap<>();
            for (Map.Entry<String, List<ChatMessage>> sessionEntry : sessionMessages.entrySet()) {
                List<Map<String, String>> sessionMsgs = new ArrayList<>();
                for (ChatMessage msg : sessionEntry.getValue()) {
                    Map<String, String> m = new java.util.HashMap<>();
                    if (msg instanceof UserMessage) {
                        m.put("type", "user");
                        m.put("content", ((UserMessage) msg).singleText());
                    } else if (msg instanceof AiMessage) {
                        m.put("type", "assistant");
                        m.put("content", ((AiMessage) msg).text());
                    }
                    sessionMsgs.add(m);
                }
                msgsData.put(sessionEntry.getKey(), sessionMsgs);
            }
            root.put("messages", msgsData);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(MEMORY_FILE_PATH), root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
