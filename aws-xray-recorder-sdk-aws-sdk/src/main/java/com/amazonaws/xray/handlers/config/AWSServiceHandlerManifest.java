package com.amazonaws.xray.handlers.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSServiceHandlerManifest {
    @JsonProperty
    private Map<String, AWSOperationHandlerManifest> services;

    public AWSOperationHandlerManifest getOperationHandlerManifest(String serviceName) {
        return services.get(serviceName);
    }
}
