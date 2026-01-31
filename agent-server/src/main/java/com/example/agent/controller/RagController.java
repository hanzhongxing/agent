package com.example.agent.controller;

import com.example.agent.service.RagService;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/ingest")
    public String ingest(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        ragService.ingest(content);
        return "Ingested";
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam String query) {
        return ragService.search(query).stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());
    }
}
