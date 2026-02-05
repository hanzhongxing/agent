package com.example.agent.service;

import com.example.agent.model.McpTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class McpServiceTest {

    @Autowired
    private McpService mcpService;

    @Test
    public void doTest(){
        List<McpTool> tools=mcpService.getTools("a16befce-10a1-4f95-b9fc-829e5fad056c");
        if(tools!=null){
            System.out.println();
        }
    }

}
