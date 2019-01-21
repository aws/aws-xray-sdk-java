package com.amazonaws.xray.handlers.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AWSOperationHandlerManifest {
    @JsonProperty
    private Map<String, AWSOperationHandler> operations;

    public AWSOperationHandler getOperationHandler(String operationRequestClassName) {
        return operations.get(operationRequestClassName);
    }
}
