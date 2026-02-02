package com.example.agent.controller;

import com.example.agent.model.ModelConfig;
import com.example.agent.service.MemoryService;
import com.example.agent.service.ModelConfigService;
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
    private ModelConfigService modelConfigService;

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

        ModelConfig config = modelConfigService.getConfig(modelId);
        if (config == null) {
            return Flux.error(new IllegalArgumentException("Model configuration not found for ID: " + modelId));
        }

        StreamingChatLanguageModel client = createStreamingChatModel(config);

        // 1. Prepare Tools
        List<ToolSpecification> tools = mcpService.getEnabledTools();

        // 2. Prepare Prompt & Context
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
            generateResponse(client, chatMessages, tools, sink, sessionId, useMemory, message);
        });
    }

    private void generateResponse(
            StreamingChatLanguageModel client,
            List<ChatMessage> messages,
            List<ToolSpecification> tools,
            FluxSink<String> sink,
            String sessionId,
            boolean useMemory,
            String originalUserMessage
    ) {
        StringBuilder fullResponse = new StringBuilder();
        logger.info("Generating with {} tools", tools.size());

        client.generate(messages, tools, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                sink.next(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                AiMessage aiMessage = response.content();

                if (aiMessage.hasToolExecutionRequests()) {
                    // TOOL EXECUTION DETECTED
                    messages.add(aiMessage); // Add the assistant's request to history

                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        String toolName = toolRequest.name();
                        String args = toolRequest.arguments();

                        // Notify frontend we are executing (optional, sends a special token or log)
                        sink.next("\n[Executing tool: " + toolName + "]\n");

                        String result = mcpService.executeTool(toolName, args);

                        messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    }

                    // RECURSIVE CALL with new history (Loop)
                    generateResponse(client, messages, tools, sink, sessionId, useMemory, originalUserMessage);

                } else {
                    // FINAL RESPONSE
                    if (useMemory) {
                        memoryService.addMessage(sessionId, new UserMessage(originalUserMessage));
                        memoryService.addMessage(sessionId, aiMessage);
                    }
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

    private StreamingChatLanguageModel createStreamingChatModel(ModelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
