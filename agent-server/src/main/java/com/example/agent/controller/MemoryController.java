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
    public List<Map<String, String>> getMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = memoryService.getMessages(sessionId);
        return messages.stream().map(msg -> {
            Map<String, String> m = new HashMap<>();

            if (msg instanceof UserMessage) {
                m.put("role", "user");
                m.put("content", ((UserMessage) msg).singleText());
            } else if (msg instanceof ToolExecutionResultMessage) {
                // 专门处理工具结果消息
                ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) msg;
                m.put("role", "tool");
                m.put("toolName", toolMsg.toolName());
                m.put("output", toolMsg.text());
                m.put("content", "Tool execution completed."); // 简略显示的文本
            } else if (msg instanceof AiMessage) {
                AiMessage aiMsg = (AiMessage) msg;
                if (aiMsg.hasToolExecutionRequests()) {
                    // 这是 AI 发起的工具调用请求
                    m.put("role", "assistant_tool_request");
                    // 获取工具调用的详细参数
                    var requests = aiMsg.toolExecutionRequests().stream().map(req -> {
                        Map<String, String> toolInfo = new HashMap<>();
                        toolInfo.put("name", req.name());
                        toolInfo.put("arguments", req.arguments());
                        return toolInfo;
                    }).toList();
                    m.put("toolCalls", requests.toString());
                    m.put("content", "Calling tools...");
                } else {
                    // 普通 AI 回复
                    m.put("role", "assistant");
                    m.put("content", aiMsg.text());
                }
            }
            return m;
        }).collect(Collectors.toList());
    }
}
