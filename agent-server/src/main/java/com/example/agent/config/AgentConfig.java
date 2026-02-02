package com.example.agent.config;

import com.example.agent.util.AgentFileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Value("${agent.base.path}")
    private String basePath;

    @Value("${agent.llm.folder}")
    private String llmFolder;

    @Value("${agent.llm.file}")
    private String llmFile;

    @Value("${agent.session.folder}")
    private String sessionFolder;

    @Value("${agent.session.file}")
    private String sessionFile;

    @Value("${agent.memory.folder}")
    private String memoryFolder;

    @Value("${agent.memory.file}")
    private String memoryFile;

    @Value("${agent.rag.folder}")
    private String ragFolder;

    @Value("${agent.mcp.folder}")
    private String mcpFolder;

    @Value("${agent.mcp.file}")
    private String mcpFile;

    public String getLlmFilePath() {
        return AgentFileUtil.getFilePath(basePath, llmFolder, llmFile);
    }

    public String getSessionFilePath() {
        return AgentFileUtil.getFilePath(basePath, sessionFolder, sessionFile);
    }

    public String getMemoryFilePath() {
        return AgentFileUtil.getFilePath(basePath, memoryFolder, memoryFile);
    }

    public String getRAGFilePath() {
        return AgentFileUtil.getFilePath(basePath, ragFolder);
    }

    public String getMcpFilePath() {
        return AgentFileUtil.getFilePath(basePath, mcpFolder, mcpFile);
    }
}