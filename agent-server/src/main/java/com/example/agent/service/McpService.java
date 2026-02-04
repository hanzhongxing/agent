package com.example.agent.service;

import com.example.agent.config.AgentConfig;
import com.example.agent.model.BaseRes;
import com.example.agent.model.McpInfo;
import com.example.agent.model.McpTool;
import com.example.agent.util.mcp.CustomMcpClient;
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

@Service
public class McpService extends BaseService{
    private final static Logger logger = LoggerFactory.getLogger(McpService.class);

    @Autowired
    private AgentConfig agentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<McpInfo> mcpInfos = new CopyOnWriteArrayList<>();
    private final RestClient restClient = RestClient.create();

    @PostConstruct
    public void init() {
        loadConfigs();
    }

    public List<McpInfo> getAllConfigs() {
        return new ArrayList<>(mcpInfos);
    }

    public McpInfo addConfig(McpInfo config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        mcpInfos.add(config);
        saveConfigs();
        return config;
    }

    public void updateConfig(McpInfo config) {
        for (int i = 0; i < mcpInfos.size(); i++) {
            if (mcpInfos.get(i).getId().equals(config.getId())) {
                mcpInfos.set(i, config);
                saveConfigs();
                return;
            }
        }
    }

    public void deleteConfig(String id) {
        mcpInfos.removeIf(c -> c.getId().equals(id));
        saveConfigs();
    }

    private void loadConfigs() {
        File file = new File(agentConfig.getMcpFilePath());
        if (file.exists()) {
            try {
                List<McpInfo> loaded = objectMapper.readValue(file, new TypeReference<List<McpInfo>>() {});
                mcpInfos.clear();
                mcpInfos.addAll(loaded);
            } catch (IOException e) {
                logger.error("Failed to load MCP configs", e);
            }
        }
    }

    private synchronized void saveConfigs() {
        try {
            File file = new File(agentConfig.getMcpFilePath());
            if (file.getParentFile() != null) {
                boolean flg=file.getParentFile().mkdirs();
                if(!flg){
                    logger.error("Failed to save MCP configs file getParentFile is null");
                    return;
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, mcpInfos);
        } catch (IOException e) {
            logger.error("Failed to save MCP configs", e);
        }
    }

    public List<ToolSpecification> getAllTools() {
        List<ToolSpecification> specs = new ArrayList<>();
        for (McpInfo config : mcpInfos) {
            if (!config.isEnabled()) {
                continue;
            }
            List<ToolSpecification> tools=CustomMcpClient.getTools4Chat(config.getBaseUrl());
            if(!tools.isEmpty()){
                specs.addAll(tools);
            }
        }
        return specs;
    }

    public List<McpTool> getTools(String id){
        for (McpInfo config : mcpInfos) {
            if (!config.isEnabled()) {
                continue;
            }
            if(!config.getId().equals(id)){
                continue;
            }
            return CustomMcpClient.getTools4Show(config.getBaseUrl());
        }
        return null;
    }

    public String callTools(String id,String name,String inputs){
        McpInfo mcpInfo=getMcpInfo(id);
        if(mcpInfo==null){
            return BaseRes.Error(50001,"未找到对应的mcp服务");
        }
        McpTool mcpTool=getTool(mcpInfo,name);
        if(mcpTool==null){
            return BaseRes.Error(50001,"未找到注册的tool");
        }
        return callMcpTool(mcpInfo,mcpTool,inputs);
    }

    private McpInfo getMcpInfo(String id){
        for (McpInfo config : mcpInfos) {
            if (!config.isEnabled()) {
                continue;
            }
            if(config.getId().equals(id)){
                return config;
            }
        }
        return null;
    }

    /**
     * Executes a tool on the appropriate MCP server.
     */
    public String executeTool(String toolName, String arguments) {
        for (McpInfo config : mcpInfos) {
            if (config.isEnabled()) {
                McpTool mcpTool=getTool(config,toolName);
                if(mcpTool==null){
                    continue;
                }
                return callMcpTool(config,mcpTool,arguments);
            }
        }
        return "Error: Tool " + toolName + " not found or execution failed on all MCP servers.";
    }

    private String callMcpTool(McpInfo mcpInfo,McpTool mcpTool,String arguments){
        return CustomMcpClient.callTool(mcpInfo.getBaseUrl(),mcpTool.getName(),arguments);
    }

    private McpTool getTool(McpInfo mcpInfo,String name){
        List<McpTool> tools= CustomMcpClient.getTools4Show(mcpInfo.getBaseUrl());
        if(tools.isEmpty()){
            return null;
        }
        for(McpTool mcpTool:tools){
            if(mcpTool.getName().equals(name)){
                return mcpTool;
            }
        }
        return null;
    }
}
