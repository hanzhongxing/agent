package com.example.agent.controller;

import com.example.agent.model.ChatSession;
import com.example.agent.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionController {
    private final static Logger logger= LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    @GetMapping("/sessions")
    public List<ChatSession> getSessions() {
        logger.info("Fetching all sessions");
        return sessionService.getSessions();
    }

    @PostMapping("/sessions")
    public void saveSession(@RequestBody ChatSession session) {
        logger.info("Saving session: {}", session);
        sessionService.updateSession(session);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        logger.info("Deleting session: {}", sessionId);
        sessionService.deleteSession(sessionId);
    }

}
