package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.ChatSession;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final static Logger logger= LoggerFactory.getLogger(SessionService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private MemoryService memoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String,ChatSession> sessionMetadata = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        loadSessions();
    }

    public ChatSession getSession(String sessionId){
        return sessionMetadata.get(sessionId);
    }

    public List<ChatSession> getSessions() {
        return new ArrayList<>(sessionMetadata.values());
    }

    public void addSession(ChatSession session) {
        if (session.getId() != null) {
            sessionMetadata.put(session.getId(), session);
            saveSessions();
        }
    }

    public void updateSession(ChatSession session) {
        if (session.getId() != null) {
            sessionMetadata.put(session.getId(), session);
            saveSessions();
        }
    }

    public void deleteSession(String sessionId) {
        memoryService.removeMessage(sessionId);
        sessionMetadata.remove(sessionId);
        saveSessions();
    }

    private void loadSessions() {
        File file = new File(agentConfig.getSessionFilePath());
        if (file.exists()) {
            try {
                Map<String, ChatSession> loaded = objectMapper.readValue(file,
                        new TypeReference<Map<String, ChatSession>>() {
                        });
                sessionMetadata.putAll(loaded);
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
        }
    }

    public synchronized void saveSessions() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(agentConfig.getSessionFilePath()), sessionMetadata);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
