package com.example.mcp.model;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelContext {

    private Map<String, String> data;

    public ModelContext() {
        this.data = new HashMap<>();
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public void put(String key, String value) {
        this.data.put(key, value);
    }

    public String get(String key) {
        return this.data.get(key);
    }

    public String toJsonString() {
        StringBuilder jsonBuilder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                jsonBuilder.append(","); 
            }
            jsonBuilder.append("\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public static ModelContext fromJsonString(String jsonString) {
        ModelContext context = new ModelContext();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty.");
        }
        String trimmedJson = jsonString.trim();
        if (!trimmedJson.startsWith("{") || !trimmedJson.endsWith("}")) {
            throw new IllegalArgumentException("JSON string must start with '{' and end with '}'.");
        }
        if (trimmedJson.equals("{}")) {
            return context; // Empty JSON object is valid
        }

        String keyValuePairsString = trimmedJson.substring(1, trimmedJson.length() - 1).trim();
        if (keyValuePairsString.isEmpty()) {
            return context; // Case like "{   }" which is a valid empty object
        }

        Pattern pattern = Pattern.compile("\"(.*?)\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(keyValuePairsString);

        int lastEnd = 0;
        boolean foundMatch = false;
        while (matcher.find()) {
            foundMatch = true;
            // Check for unexpected characters between matches
            String betweenContent = keyValuePairsString.substring(lastEnd, matcher.start()).trim();
            if (lastEnd == 0) { // Before the first pair
                if (!betweenContent.isEmpty()) {
                    throw new IllegalArgumentException("Invalid JSON format: Unexpected characters before first key-value pair: '" + betweenContent + "'");
                }
            } else { // Between subsequent pairs
                if (!betweenContent.equals(",")) {
                    throw new IllegalArgumentException("Invalid JSON format: Expected ',' separator, found: '" + betweenContent + "' near " + keyValuePairsString.substring(lastEnd, matcher.start()));
                }
            }
            
            try {
                String key = matcher.group(1); 
                String value = matcher.group(2);
                // TODO: Implement unescaping for key and value if complex JSON strings are expected
                context.put(key, value);
            } catch (Exception e) { 
                throw new IllegalArgumentException("Error parsing key-value pair: " + matcher.group(0), e);
            }
            lastEnd = matcher.end();
        }

        if (!foundMatch) { 
             throw new IllegalArgumentException("Invalid JSON format: No valid key-value pairs found in '" + keyValuePairsString + "'");
        }
        if (lastEnd < keyValuePairsString.length() && !keyValuePairsString.substring(lastEnd).trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON format: Trailing characters after last key-value pair: '" + keyValuePairsString.substring(lastEnd).trim() + "'");
        }
        return context;
    }
}
