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

package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Plugin {

    /**
     * Returns the name of the origin associated with this plugin.
     * The {@link com.amazonaws.xray.AWSXRayRecorder} contains a prioritized list of origins from least to most specific.
     *
     * @return the name of the origin associated with this plugin.
     */
    String getOrigin();

    String getServiceName();

    /**
     * @return true if an environment inspection determines X-Ray is operating in the correct environment for this plugin OR
     * if X-Ray cannot accurately determine if it's in this plugin's environment
     */
    default boolean isEnabled() {
        return true;
    }

    Map<String, @Nullable Object> getRuntimeContext();

    default Set<AWSLogReference> getLogReferences() {
        return Collections.emptySet();
    }

}
