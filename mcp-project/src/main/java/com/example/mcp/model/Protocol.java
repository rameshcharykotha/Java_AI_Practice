package com.example.mcp.model;

public class Protocol {

    public static final String LOAD_MODEL_PREFIX = "LOAD_MODEL:";
    public static final String GET_CONTEXT_PREFIX = "GET_CONTEXT:";
    public static final String UPDATE_CONTEXT_PREFIX = "UPDATE_CONTEXT:";
    public static final String SUCCESS_PREFIX = "SUCCESS:";
    public static final String ERROR_PREFIX = "ERROR:";
    public static final String CONTEXT_DATA_PREFIX = "CONTEXT_DATA:";

    private Protocol() {
        // Private constructor to prevent instantiation
    }
}
