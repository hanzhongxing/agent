package com.example.agent.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BaseRes {
    private final static Logger logger= LoggerFactory.getLogger(BaseRes.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();

    private final static String common_error="系统异常";

    public static String Error(int code,String message){
        try {
            Map<String, Object> res = new HashMap<>();
            res.put("code", code);
            res.put("message", message);
            return objectMapper.writeValueAsString(res);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return common_error;
    }
}
