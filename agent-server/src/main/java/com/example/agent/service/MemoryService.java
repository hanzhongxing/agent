package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.ChatSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final static Logger logger= LoggerFactory.getLogger(MemoryService.class);

    @Resource
    private AgentConfig agentConfig;

    @Autowired
    private SessionService sessionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, List<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGES = 50;

    @PostConstruct
    public void init() {
        loadMessages();
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
        ChatSession session=sessionService.getSession(sessionId);
        if (session==null) {
            sessionService.addSession(ChatSession.builder()
                    .id(sessionId)
                    .title("Conversation " + sessionId)
                    .useMemory(true)
                    .useRag(false)
                    .build());
        }
        saveMessages();
    }

    public void removeMessage(String sessionId){
        sessionMessages.remove(sessionId);
        saveMessages();
    }

    private void loadMessages() {
        File file = new File(agentConfig.getMemoryFilePath());
        if (file.exists()) {
            try {
                Map<String, List<Map<String, String>>> root = objectMapper.readValue(file,
                        new TypeReference<Map<String, List<Map<String, String>>>>() {
                        });
                for (Map.Entry<String, List<Map<String, String>>> sessionEntry : root.entrySet()) {
                    List<ChatMessage> messages = new CopyOnWriteArrayList<>();
                    for (Map<String, String> entry : sessionEntry.getValue()) {
                        String type = entry.get("type");
                        String content = entry.get("content");
                        if ("user".equals(type)) {
                            messages.add(new UserMessage(content));
                        } else if ("assistant".equals(type)) {
                            messages.add(new AiMessage(content));
                        }
                    }
                    sessionMessages.put(sessionEntry.getKey(), messages);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    private synchronized void saveMessages() {
        try {
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(agentConfig.getMemoryFilePath()), msgsData);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }

}
