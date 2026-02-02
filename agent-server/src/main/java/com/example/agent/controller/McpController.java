package com.example.agent.controller;

import com.example.agent.model.McpConfig;
import com.example.agent.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*")
public class McpController {

    @Autowired
    private McpService mcpService;

    @GetMapping
    public List<McpConfig> getConfigs() {
        return mcpService.getAllConfigs();
    }

    @PostMapping
    public McpConfig addConfig(@RequestBody McpConfig config) {
        return mcpService.addConfig(config);
    }

    @PutMapping("/{id}")
    public void updateConfig(@PathVariable String id, @RequestBody McpConfig config) {
        config.setId(id);
        mcpService.updateConfig(config);
    }

    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable String id) {
        mcpService.deleteConfig(id);
    }
}