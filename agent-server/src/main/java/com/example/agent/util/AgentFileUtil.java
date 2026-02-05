package com.example.agent.util;

public class AgentFileUtil {

    public static String getFilePath(String... paths){
        StringBuilder path= new StringBuilder();
        for(String p:paths){
            path.append(p).append("/");
        }
        return path.substring(0,path.length()-1);
    }
}
