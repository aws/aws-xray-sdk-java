package com.amazonaws.xray.handlers.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSServiceHandler {
    @JsonProperty
    private Map<String, AWSOperationHandler> operations;

    public AWSOperationHandler getOperationHandler(String operationName) {
        return operations.get(operationName);
    }
}
