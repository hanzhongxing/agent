package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelConfig {
    private String id;
    private String name;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean embed;
}
