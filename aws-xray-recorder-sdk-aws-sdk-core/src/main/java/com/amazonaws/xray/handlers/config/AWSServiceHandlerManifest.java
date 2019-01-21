package com.amazonaws.xray.handlers.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AWSServiceHandlerManifest {
    @JsonProperty
    private Map<String, AWSOperationHandlerManifest> services;

    public AWSOperationHandlerManifest getOperationHandlerManifest(String serviceName) {
        return services.get(serviceName);
    }
}
