package com.example.agent.util.mcp;

import com.example.agent.model.McpTool;
import com.example.agent.model.McpToolInput;
import com.example.agent.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.net.http.HttpRequest;
import java.util.*;

public class CustomMcpClient {
    private final static Logger logger= LoggerFactory.getLogger(CustomMcpClient.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static List<McpTool> getTools4Show(String baseUrl){
        List<McpSchema.Tool> tools = getTools(baseUrl);
        return buildTools4Show(tools);
    }

    public static List<ToolSpecification> getTools4Chat(String baseUrl){
        List<McpSchema.Tool> tools = getTools(baseUrl);
        return buildTools4Chat(tools);
    }

    public static McpSchema.CallToolResult callTool(String baseUrl,String toolName,String inputs){
        HttpClientStreamableHttpTransport TRANSPORT = HttpClientStreamableHttpTransport.builder(baseUrl).
                requestBuilder(HttpRequest.newBuilder().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).resumableStreams(true).build();
        McpSyncClient client = McpClient.sync(TRANSPORT).capabilities(McpSchema.ClientCapabilities.builder().build()).build();
        client.initialize();
        client.ping();
        McpSchema.CallToolRequest callToolRequest = McpSchema.CallToolRequest.builder().arguments(new JacksonMcpJsonMapper(objectMapper),inputs)
                .name(toolName).meta(Map.of("oa", "xxx")).build();
        return client.callTool(callToolRequest);
    }

    public static String callTool4Text(String baseUrl,String toolName,String inputs){
        McpSchema.CallToolResult callToolResult =callTool(baseUrl,toolName,inputs) ;
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.toolExecutionResultMessage(UUID.randomUUID().toString(),
                toolName,String.valueOf(callToolResult.content()));
        return StringUtils.extractText(toolExecutionResultMessage.text());
    }

    private static List<McpSchema.Tool> getTools(String baseUrl){
        try {
            HttpClientStreamableHttpTransport TRANSPORT = HttpClientStreamableHttpTransport.builder(baseUrl).
                    requestBuilder(HttpRequest.newBuilder().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).resumableStreams(true).build();
            McpSyncClient client = McpClient.sync(TRANSPORT).capabilities(McpSchema.ClientCapabilities.builder().build()).build();
            client.initialize();
            client.ping();
            List<McpSchema.Tool> tools = client.listTools().tools();
            TRANSPORT.closeGracefully();
            client.close();
            return tools;
        }catch (Exception e){
            logger.debug(e.getMessage(),e);
        }
        return null;
    }

    private static List<ToolSpecification> buildTools4Chat(List<McpSchema.Tool> tools){
        List<ToolSpecification> mcpTools=new ArrayList<>();
        if(tools==null||tools.isEmpty()){
            return mcpTools;
        }
        for(McpSchema.Tool tool:tools){
            mcpTools.add(ToolSpecification.builder()
                            .name(tool.name())
                            .description(tool.description())
                            .parameters(ToolParameters.builder()
                                    .type(tool.inputSchema().type())
                                    .properties(buildInputs4Chat(tool.inputSchema()))
                                    .build()
                            )
                            .build());
        }
        return mcpTools;
    }

    private static List<McpTool> buildTools4Show(List<McpSchema.Tool> tools){
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
            mcpTool.setInputs(buildInputs4Show(tool.inputSchema()));
            mcpTools.add(mcpTool);
        }
        return mcpTools;
    }

    private static Map<String,Map<String,Object>> buildInputs4Chat(McpSchema.JsonSchema jsonSchema){
        Map<String,Map<String,Object>> map=new HashMap<>();
        if(jsonSchema==null){
            return map;
        }
        String schemeType=jsonSchema.type();
        if(!"object".equals(schemeType)){
            return map;
        }
        Map<String, Object> properties=jsonSchema.properties();
        if(properties==null||properties.isEmpty()){
            return map;
        }
        for(String key:properties.keySet()){
            if(!map.containsKey(key)){
                map.put(key,new HashMap<String,Object>());
            }
            Map<String,Object> values=(Map<String,Object>)properties.get(key);
            map.get(key).put("type",values.get("type"));
            map.get(key).put("description",values.get("description"));
        }
        return map;
    }

    private static List<McpToolInput> buildInputs4Show(McpSchema.JsonSchema jsonSchema){
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
