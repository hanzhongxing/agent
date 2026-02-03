package com.example.agent.util.mcp;

import com.example.agent.model.McpTool;
import com.example.agent.model.McpToolInput;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMcpClient {

    public static List<McpTool> getTools(String baseUrl){
        HttpClientStreamableHttpTransport TRANSPORT = HttpClientStreamableHttpTransport.builder(baseUrl).
                requestBuilder(HttpRequest.newBuilder().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).resumableStreams(true).build();
        McpSyncClient client = McpClient.sync(TRANSPORT).capabilities(McpSchema.ClientCapabilities.builder().build()).build();
        client.initialize();
        client.ping();
        List<McpSchema.Tool> tools = client.listTools().tools();
        TRANSPORT.closeGracefully();
        client.close();
        return buildTools(tools);
    }

    private static List<McpTool> buildTools(List<McpSchema.Tool> tools){
        List<McpTool> mcpTools=new ArrayList<>();
        if(tools==null||tools.isEmpty()){
            return mcpTools;
        }
        McpTool mcpTool=null;
        for(McpSchema.Tool tool:tools){
            mcpTool=new McpTool();
            mcpTool.setName(tool.name());
            mcpTool.setTitle(tool.title());
            mcpTool.setDescription(tool.description());
            mcpTool.setInputs(buildInputs(tool.inputSchema()));
            mcpTools.add(mcpTool);
        }
        return mcpTools;
    }

    private static List<McpToolInput> buildInputs(McpSchema.JsonSchema jsonSchema){
        List<McpToolInput> mcpToolInputs=new ArrayList<>();
        if(jsonSchema==null){
            return mcpToolInputs;
        }
        String schemeType=jsonSchema.type();
        if(!"object".equals(schemeType)){
            return mcpToolInputs;
        }
        Map<String, Object> properties=jsonSchema.properties();
        if(properties==null||properties.isEmpty()){
            return mcpToolInputs;
        }
        McpToolInput toolInput=null;
        for(String key:properties.keySet()){
            toolInput=new McpToolInput();
            toolInput.setField(key);
            toolInput.setRequired(jsonSchema.required().contains(key));
            Map<String,Object> values=(Map<String,Object>)properties.get(key);
            toolInput.setType(values.get("type").toString());
            toolInput.setDesc(values.get("description").toString());
            mcpToolInputs.add(toolInput);
        }
        return mcpToolInputs;
    }
}
