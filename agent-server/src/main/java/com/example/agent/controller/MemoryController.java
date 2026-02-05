package com.example.agent.controller;

import com.example.agent.service.MemoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
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
    public List<Map<String, Object>> getMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = memoryService.getMessages(sessionId);
        return messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>(); // 修改为 Object
            if (msg instanceof UserMessage) {
                m.put("role", "user");
                m.put("content", ((UserMessage) msg).singleText());
            } else if (msg instanceof ToolExecutionResultMessage toolMsg) {
                m.put("role", "tool");
                m.put("toolName", toolMsg.toolName());
                m.put("output", toolMsg.text());
                m.put("content", "Tool execution completed.");
            } else if (msg instanceof AiMessage aiMsg) {
                if (aiMsg.hasToolExecutionRequests()) {
                    m.put("role", "assistant_tool_request");
                    var requests = aiMsg.toolExecutionRequests().stream().map(req -> {
                        Map<String, String> toolInfo = new HashMap<>();
                        toolInfo.put("name", req.name());
                        toolInfo.put("arguments", req.arguments()); // 参数保持原样
                        return toolInfo;
                    }).toList();
                    m.put("toolCalls", requests);
                    m.put("content", "Calling tools...");
                } else {
                    m.put("role", "assistant");
                    m.put("content", aiMsg.text());
                }
            }
            return m;
        }).collect(Collectors.toList());
    }
}
