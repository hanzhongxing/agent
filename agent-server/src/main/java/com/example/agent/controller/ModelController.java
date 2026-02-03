package com.example.agent.controller;

import com.example.agent.model.ModelInfo;
import com.example.agent.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping
    public List<ModelInfo> getModels() {
        return modelService.getAllConfigs();
    }

    @GetMapping("/chat")
    public List<ModelInfo> getModels4Chat() {
        return modelService.getChatConfigs();
    }


    @PostMapping
    public ModelInfo addModel(@RequestBody ModelInfo config) {
        return modelService.addConfig(config);
    }

    @PutMapping("/{id}")
    public void updateModel(@PathVariable String id, @RequestBody ModelInfo config) {
        config.setId(id);
        modelService.updateConfig(config);
    }

    @DeleteMapping("/{id}")
    public void deleteModel(@PathVariable String id) {
        modelService.deleteConfig(id);
    }
}
