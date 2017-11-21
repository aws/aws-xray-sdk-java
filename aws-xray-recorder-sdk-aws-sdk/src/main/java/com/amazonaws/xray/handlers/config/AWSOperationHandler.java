package com.amazonaws.xray.handlers.config;

import java.util.HashMap;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSOperationHandler {
    @JsonProperty
    private HashMap<String, AWSOperationHandlerRequestDescriptor> requestDescriptors;

    @JsonProperty
    private HashSet<String> requestParameters;

    @JsonProperty
    private HashMap<String, AWSOperationHandlerResponseDescriptor> responseDescriptors;

    @JsonProperty
    private HashSet<String> responseParameters;

    @JsonProperty
    private String requestIdHeader;

    /**
     * @return the requestKeys
     */
    public HashMap<String, AWSOperationHandlerRequestDescriptor> getRequestDescriptors() {
        return requestDescriptors;
    }

    /**
     * @return the requestParameters
     */
    public HashSet<String> getRequestParameters() {
        return requestParameters;
    }

    /**
     * @return the responseDescriptors
     */
    public HashMap<String, AWSOperationHandlerResponseDescriptor> getResponseDescriptors() {
        return responseDescriptors;
    }

    /**
     * @return the responseParameters
     */
    public HashSet<String> getResponseParameters() {
        return responseParameters;
    }

    /**
     * @return the requestIdHeader
     */
    public String getRequestIdHeader() {
        return requestIdHeader;
    }
}
