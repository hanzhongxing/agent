package com.example.agent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class StringUtils extends org.springframework.util.StringUtils {

    private final static List<String> meaninglessValues = Arrays.asList(
            "null", "undefined", "NULL", "UNDEFINED","nil", "NIL", "na", "NA", "NaN"
    );


    public static String extractText(String input) {
        try {
            if (input == null || input.isEmpty()) {
                return null;
            }
            String jsonStr = extractJson(input);
            if(!hasText(jsonStr)){
                return null;
            }
            return jsonStr;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return null;
    }

    public static String extractValue(String input,String name) {
        try {
            if (input == null || input.isEmpty()) {
                return null;
            }
            String jsonStr = extractJson(input);
            if(!hasText(jsonStr)){
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonStr);
            String res=rootNode.get(name).asText();
            if(!hasText(res)||isMeaningless(res)){
                return null;
            }
            return res;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return null;
    }

    private static boolean isMeaningless(String str) {
        if (str == null) {
            return true;
        }
        String trimmed = str.trim();
        return meaninglessValues.contains(trimmed.toLowerCase());
    }

    private static String extractJson(String text) {
        int textStart = text.indexOf("text=");
        if (textStart == -1) {
            return null;
        }
        int jsonStart = text.indexOf('{', textStart);
        if (jsonStart == -1) {
            return null;
        }
        int braceCount = 0;
        int jsonEnd = -1;
        for (int i = jsonStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    jsonEnd = i;
                    break;
                }
            }
        }
        if (jsonEnd != -1) {
            return text.substring(jsonStart, jsonEnd + 1);
        }
        return null;
    }
}
