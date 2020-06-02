/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
