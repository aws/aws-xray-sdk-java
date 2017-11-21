package com.amazonaws.xray.handlers.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSOperationHandlerManifest {
    @JsonProperty
    private Map<String, AWSOperationHandler> operations;

    public AWSOperationHandler getOperationHandler(String operationRequestClassName) {
        return operations.get(operationRequestClassName);
    }
}
