package com.amazonaws.xray.handlers.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AWSServiceHandler {
    @JsonProperty
    private Map<String, AWSOperationHandler> operations;

    public AWSOperationHandler getOperationHandler(String operationName) {
        return operations.get(operationName);
    }
}
