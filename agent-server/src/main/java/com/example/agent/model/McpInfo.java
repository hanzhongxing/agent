package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpInfo {
    private String id;
    private String name;
    private String baseUrl; // The URL of the MCP server (e.g., http://localhost:3000)
    private boolean enabled;
}
