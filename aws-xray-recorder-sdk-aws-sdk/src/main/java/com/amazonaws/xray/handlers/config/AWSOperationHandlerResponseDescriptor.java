package com.amazonaws.xray.handlers.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AWSOperationHandlerResponseDescriptor {
    @JsonProperty
    private String renameTo;

    @JsonProperty
    private boolean map = false;

    @JsonProperty
    private boolean list = false;

    @JsonProperty
    private boolean getKeys = false;

    @JsonProperty
    private boolean getCount = false;

    /**
     * @return the renameTo
     */
    public String getRenameTo() {
        return renameTo;
    }

    /**
     * @return the map
     */
    public boolean isMap() {
        return map;
    }

    /**
     * @return the list
     */
    public boolean isList() {
        return list;
    }

    /**
     * @return the getCount
     */
    public boolean shouldGetCount() {
        return getCount;
    }

    /**
     * @return the getKeys
     */
    public boolean shouldGetKeys() {
        return getKeys;
    }
}
