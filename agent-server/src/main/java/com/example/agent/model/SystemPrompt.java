package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemPrompt {
    private String id;
    private String name;
    private String content;
    private boolean active; // 是否启用
}
