package com.example.agent.controller;

import com.example.agent.model.ModelInfo;
import com.example.agent.service.MemoryService;
import com.example.agent.service.ModelService;
import com.example.agent.service.RagService;
import com.example.agent.service.McpService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    private final static Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private RagService ragService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private McpService mcpService;

    @PostMapping(produces = "text/event-stream")
    public Flux<String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        boolean useRag = Boolean.TRUE.equals(request.get("useRag"));
        boolean useMemory = Boolean.TRUE.equals(request.get("useMemory"));
        String modelId = (String) request.get("modelId");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        if (modelId == null || modelId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("Model ID is required."));
        }
        ModelInfo config = modelService.getConfig(modelId);
        if (config == null) {
            return Flux.error(new IllegalArgumentException("Model configuration not found for ID: " + modelId));
        }
        StreamingChatLanguageModel client = createStreamingChatModel(config);
        List<ToolSpecification> tools = mcpService.getAllTools();
        String prompt = message;
        if (useRag) {
            List<TextSegment> docs = ragService.search(message);
            if (docs != null && !docs.isEmpty()) {
                String context = docs.stream().map(TextSegment::text).collect(Collectors.joining("\n"));
                prompt = "Use the following context to answer the question:\n" + context + "\n\nQuestion: " + message;
            }
        }
        final List<ChatMessage> chatMessages = new ArrayList<>();
        if (useMemory) {
            chatMessages.addAll(memoryService.getMessages(sessionId));
        }
        chatMessages.add(new UserMessage(prompt));
        return Flux.create(sink -> {
            memoryService.addMessage(sessionId, new UserMessage(message));
            generateResponse(client, chatMessages, tools, sink, sessionId);
        });
    }

    private void generateResponse(
            StreamingChatLanguageModel client,
            List<ChatMessage> messages,
            List<ToolSpecification> tools,
            FluxSink<String> sink,
            String sessionId
    ) {
        logger.info("Generating with {} tools", tools.size());
        client.generate(messages, tools, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                sink.next(token);
            }
            @Override
            public void onComplete(Response<AiMessage> response) {
                AiMessage aiMessage = response.content();
                if (aiMessage.hasToolExecutionRequests()) {
                    // Tool Execution Logic
                    messages.add(aiMessage);
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        memoryService.addMessage(sessionId,aiMessage);
                        String toolName = toolRequest.name();
                        String args = toolRequest.arguments();
                        // 【关键修改】发送特殊标记的JSON字符串，包含完整的输入参数
                        // 格式：:::TOOL_START:::{JSON Data}:::TOOL_END:::
                        String toolStartJson = String.format("{\"name\":\"%s\", \"args\":%s}", toolName, args.replace("\n", ""));
                        sink.next("\n:::TOOL_START:::" + toolStartJson + ":::TOOL_END:::\n");

                        String result = "Error: Tool execution failed.";
                        try {
                            result = mcpService.executeTool(toolName, args);
                        } catch (Exception e) {
                            logger.error("Error executing MCP tool {}", toolName, e);
                            result = "Error executing tool: " + e.getMessage();
                        }
                        // 【关键修改】发送结果，前端拿到后更新上面的卡片
                        // 为了避免特殊字符破坏流，这里可以考虑简单转义，或者直接发送文本
                        // 这里我们发送一个结果标记
                        sink.next(":::TOOL_OUTPUT_START:::\n" + result + "\n:::TOOL_OUTPUT_END:::\n");
                        ToolExecutionResultMessage message=ToolExecutionResultMessage.from(toolRequest, result);
                        messages.add(message);
                        memoryService.addMessage(sessionId,message);
                    }
                    generateResponse(client, messages, tools, sink, sessionId);
                } else {
                    memoryService.addMessage(sessionId, aiMessage);
                    sink.complete();
                }
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Streaming error", error);
                sink.error(error);
            }
        });
    }

    private StreamingChatLanguageModel createStreamingChatModel(ModelInfo config) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
