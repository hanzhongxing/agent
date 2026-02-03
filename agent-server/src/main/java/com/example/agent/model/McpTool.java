package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpTool {
    private String name;
    private String title;
    private String description;
    private List<McpToolInput> inputs;
}
