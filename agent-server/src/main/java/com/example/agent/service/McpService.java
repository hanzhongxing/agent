package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.McpConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class McpService {
    private final static Logger logger = LoggerFactory.getLogger(McpService.class);

    @Autowired
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<McpConfig> mcpConfigs = new CopyOnWriteArrayList<>();
    private final RestClient restClient = RestClient.create();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    // --- Config Management ---

    public List<McpConfig> getAllConfigs() {
        return new ArrayList<>(mcpConfigs);
    }

    public McpConfig addConfig(McpConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        mcpConfigs.add(config);
        saveConfigs();
        return config;
    }

    public void updateConfig(McpConfig config) {
        for (int i = 0; i < mcpConfigs.size(); i++) {
            if (mcpConfigs.get(i).getId().equals(config.getId())) {
                mcpConfigs.set(i, config);
                saveConfigs();
                return;
            }
        }
    }

    public void deleteConfig(String id) {
        mcpConfigs.removeIf(c -> c.getId().equals(id));
        saveConfigs();
    }

    private void loadConfigs() {
        File file = new File(agentConfig.getMcpFilePath());
        if (file.exists()) {
            try {
                List<McpConfig> loaded = objectMapper.readValue(file, new TypeReference<List<McpConfig>>() {});
                mcpConfigs.clear();
                mcpConfigs.addAll(loaded);
            } catch (IOException e) {
                logger.error("Failed to load MCP configs", e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            File file = new File(agentConfig.getMcpFilePath());
            // Ensure parent directory exists
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, mcpConfigs);
        } catch (IOException e) {
            logger.error("Failed to save MCP configs", e);
        }
    }

    // --- Runtime Tool Logic ---

    /**
     * Fetches all tool specifications from all enabled MCP servers.
     */
    public List<ToolSpecification> getEnabledTools() {
        List<ToolSpecification> specs = new ArrayList<>();
        for (McpConfig config : mcpConfigs) {
            if (config.isEnabled()) {
                try {
                    // Expecting MCP server to return List<ToolSpecification> JSON
                    // Schema adaptation might be needed depending on actual MCP server implementation
                    String toolsJson = restClient.get()
                            .uri(config.getBaseUrl() + "/tools")
                            .retrieve()
                            .body(String.class);
                    
                    if (toolsJson != null) {
                        List<ToolSpecification> serverTools = objectMapper.readValue(
                                toolsJson, new TypeReference<List<ToolSpecification>>() {});
                        specs.addAll(serverTools);
                    }
                } catch (Exception e) {
                    logger.error("Failed to fetch tools from MCP: " + config.getName(), e);
                }
            }
        }
        return specs;
    }

    /**
     * Executes a tool on the appropriate MCP server.
     */
    public String executeTool(String toolName, String arguments) {
        // Naive strategy: Ask all enabled servers if they have this tool and execute.
        // In a prod env, we should cache which server has which tool.
        
        for (McpConfig config : mcpConfigs) {
            if (config.isEnabled()) {
                try {
                    // Try to execute
                    // Using a generic POST structure: { "name": toolName, "arguments": argumentsJson }
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("name", toolName);
                    // LangChain4j passes arguments as a JSON string, we might need to parse it 
                    // or pass it raw depending on the MCP server expectation.
                    // Here we assume the MCP accepts the raw argument string or map.
                    try {
                        Map<String, Object> argsMap = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
                        payload.put("arguments", argsMap);
                    } catch (Exception e) {
                        payload.put("arguments", arguments); 
                    }

                    // This is a simplification. Real MCP might use SSE or JSON-RPC.
                    // We stick to the pattern established in the existing McpClientService stub.
                    return restClient.post()
                            .uri(config.getBaseUrl() + "/tools/" + toolName)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                } catch (Exception e) {
                    // Continue to next server if this one fails (assuming 404 means tool not found)
                    logger.debug("Tool {} execution failed on server {}", toolName, config.getName());
                }
            }
        }
        return "Error: Tool " + toolName + " not found or execution failed on all MCP servers.";
    }
}