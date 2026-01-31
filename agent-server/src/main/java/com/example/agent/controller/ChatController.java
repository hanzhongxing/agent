package com.example.agent.controller;

import com.example.agent.model.ModelConfig;
import com.example.agent.service.ModelConfigService;
import com.example.agent.service.RagService;
import dev.langchain4j.data.message.AiMessage;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    private final static Logger logger= LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private RagService ragService;

    @Autowired
    private ModelConfigService modelConfigService;

    @PostMapping(produces = "text/event-stream")
    public Flux<String> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        boolean useRag = Boolean.TRUE.equals(request.get("useRag"));
        String modelId = (String) request.get("modelId");

        if (modelId == null || modelId.isEmpty()) {
            return Flux.error(new IllegalArgumentException("Model ID is required."));
        }

        ModelConfig config = modelConfigService.getConfig(modelId);
        if (config == null) {
            return Flux.error(new IllegalArgumentException("Model configuration not found for ID: " + modelId));
        }

        StreamingChatLanguageModel client = createStreamingChatModel(config);

        String prompt = message;
        if (useRag) {
            List<TextSegment> docs = ragService.search(message);
            String context = docs.stream().map(TextSegment::text).collect(Collectors.joining("\n"));
            if (!context.isEmpty()) {
                prompt = "Use the following context to answer the question:\n" + context + "\n\nQuestion: " + message;
            }
        }

        final String finalPrompt = prompt;

        return Flux.create(sink -> {
            client.generate(finalPrompt, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    logger.info("onNext token:{}",token);
                    sink.next(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    logger.info("onComplete response:{}",response);
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    logger.error(error.getMessage(),error);
                    sink.error(error);
                }
            });
        });
    }

    private StreamingChatLanguageModel createStreamingChatModel(ModelConfig config) {
        if (config == null)
            throw new IllegalArgumentException("Config cannot be null");

        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
