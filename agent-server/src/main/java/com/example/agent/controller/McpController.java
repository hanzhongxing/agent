package com.example.agent.controller;

import com.example.agent.model.McpInfo;
import com.example.agent.model.McpTool;
import com.example.agent.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*")
public class McpController {

    @Autowired
    private McpService mcpService;

    @GetMapping
    public List<McpInfo> getConfigs() {
        return mcpService.getAllConfigs();
    }

    @PostMapping
    public McpInfo addConfig(@RequestBody McpInfo config) {
        return mcpService.addConfig(config);
    }

    @PutMapping("/{id}")
    public void updateConfig(@PathVariable String id, @RequestBody McpInfo config) {
        config.setId(id);
        mcpService.updateConfig(config);
    }

    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable String id) {
        mcpService.deleteConfig(id);
    }


    @GetMapping("/{id}/tools")
    public List<McpTool> getTools(@PathVariable String id) {
        return mcpService.getTools(id);
    }

    @PostMapping("/tool/call/{id}")
    public String callTool(@PathVariable String id,@RequestBody Map<String,Object> params) {
        String name=params.get("name").toString();
        String inputs=params.get("inputs").toString();
        return mcpService.callTools(id,name,inputs);
    }
}
