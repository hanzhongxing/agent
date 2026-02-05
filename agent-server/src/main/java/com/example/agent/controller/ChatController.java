package com.example.agent.controller;

import com.example.agent.model.ModelInfo;
import com.example.agent.model.SystemPrompt;
import com.example.agent.service.*;
import com.example.agent.util.StringUtils;
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
    private McpService mcpService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private SystemPromptService systemPromptService;

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

        SystemPrompt activePrompt = systemPromptService.getActivePrompt();
        if (activePrompt != null && StringUtils.hasText(activePrompt.getContent())) {
            chatMessages.add(new SystemMessage(activePrompt.getContent()));
        }

        if (useMemory) {
            chatMessages.addAll(memoryService.getMessages(sessionId));
        }

        chatMessages.add(new UserMessage(prompt));

        return Flux.create(sink -> {
            memoryService.addMessage(sessionId, new UserMessage(message));
            generateResponse(client, chatMessages, tools, sink, sessionId);
        });
    }

    private void generateResponse(StreamingChatLanguageModel client,List<ChatMessage> messages,List<ToolSpecification> tools,FluxSink<String> sink,String sessionId) {
        logger.info("Generating with {} tools", tools.size());
        client.generate(messages, tools, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                sink.next(token);
            }
            @Override
            public void onComplete(Response<AiMessage> response) {
                if(response==null){
                    sink.complete();
                    return;
                }
                AiMessage aiMessage = response.content();
                if (aiMessage.hasToolExecutionRequests()) {
                    messages.add(aiMessage);
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        logger.info("toolRequest:{}",toolRequest);
                        memoryService.addMessage(sessionId,aiMessage);
                        String toolName = toolRequest.name();
                        String args = toolRequest.arguments();
                        String toolStartJson = String.format("{\"name\":\"%s\", \"args\":%s}", toolName, args.replace("\n", ""));
                        sink.next("\n:::TOOL_START:::" + toolStartJson + ":::TOOL_END:::\n");
                        String result = "Error: Tool execution failed.";
                        try {
                            result = mcpService.executeTool(toolName, args);
                        } catch (Exception e) {
                            logger.error("Error executing MCP tool {}", toolName, e);
                            result = "Error executing tool: " + e.getMessage();
                        }
                        sink.next(":::TOOL_OUTPUT_START:::\n" + result + "\n:::TOOL_OUTPUT_END:::\n");
                        ToolExecutionResultMessage message=ToolExecutionResultMessage.from(toolRequest, result);
                        logger.info("message:{}",message);
                        messages.add(message);
                        memoryService.addMessage(sessionId,message);
                        String userMsg = StringUtils.extractValue(message.text(), "userMsg");
                        if (StringUtils.hasText(userMsg)) {
                            sink.next(userMsg);
                            sink.complete();
                            memoryService.addMessage(sessionId,new AiMessage(userMsg));
                            return;
                        }
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
