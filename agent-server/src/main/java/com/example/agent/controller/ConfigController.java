package com.example.agent.controller;

import com.example.agent.model.ModelConfig;
import com.example.agent.service.ModelConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ModelConfigService modelConfigService;

    public ConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping
    public List<ModelConfig> getModels() {
        return modelConfigService.getAllConfigs();
    }

    @GetMapping("/chat")
    public List<ModelConfig> getModels4Chat() {
        return modelConfigService.getChatConfigs();
    }


    @PostMapping
    public ModelConfig addModel(@RequestBody ModelConfig config) {
        return modelConfigService.addConfig(config);
    }

    @PutMapping("/{id}")
    public void updateModel(@PathVariable String id, @RequestBody ModelConfig config) {
        config.setId(id);
        modelConfigService.updateConfig(config);
    }

    @DeleteMapping("/{id}")
    public void deleteModel(@PathVariable String id) {
        modelConfigService.deleteConfig(id);
    }
}
