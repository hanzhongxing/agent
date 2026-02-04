package com.example.agent.controller;

import com.example.agent.model.SystemPrompt;
import com.example.agent.service.SystemPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@CrossOrigin(origins = "*")
public class SystemPromptController {

    @Autowired
    private SystemPromptService systemPromptService;

    @GetMapping
    public List<SystemPrompt> getPrompts() {
        return systemPromptService.getAllPrompts();
    }

    @PostMapping
    public SystemPrompt addPrompt(@RequestBody SystemPrompt prompt) {
        return systemPromptService.addPrompt(prompt);
    }

    @PutMapping("/{id}")
    public void updatePrompt(@PathVariable String id, @RequestBody SystemPrompt prompt) {
        prompt.setId(id);
        systemPromptService.updatePrompt(prompt);
    }

    @DeleteMapping("/{id}")
    public void deletePrompt(@PathVariable String id) {
        systemPromptService.deletePrompt(id);
    }

    @PostMapping("/{id}/activate")
    public void activatePrompt(@PathVariable String id) {
        systemPromptService.activatePrompt(id);
    }
}
