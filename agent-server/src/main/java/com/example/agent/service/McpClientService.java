package com.example.agent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class McpClientService {

    @Autowired
    private RestClient restClient;

    public String callTool(String mcpServerUrl, String toolName, Map<String, Object> arguments) {
        // Simple JSON-RPC-like call to a hypothetical HTTP MCP endpoint
        // This is a placeholder for actual MCP protocol implementation which usually
        // involves SSE or Stdio

        return restClient.post()
                .uri(mcpServerUrl + "/tools/" + toolName)
                .body(arguments)
                .retrieve()
                .body(String.class);
    }
}
