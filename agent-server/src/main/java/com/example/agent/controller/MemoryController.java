package com.example.agent.controller;

import com.example.agent.service.MemoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = "*")
public class MemoryController {
    private final Logger logger= LoggerFactory.getLogger(MemoryController.class);

    @Autowired
    private MemoryService memoryService;

    @GetMapping("/sessions/{sessionId}/messages")
    public List<Map<String, String>> getMessages(@PathVariable String sessionId) {
        return memoryService.getMessages(sessionId).stream().map(msg -> {
            Map<String, String> m = new java.util.HashMap<>();
            String role = (msg instanceof UserMessage) ? "user" : "assistant";
            String content = (msg instanceof UserMessage) ? ((UserMessage) msg).singleText() : ((AiMessage) msg).text();
            m.put("role", role);
            m.put("content", content);
            return m;
        }).collect(Collectors.toList());
    }
}
