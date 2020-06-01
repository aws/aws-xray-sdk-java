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

package com.amazonaws.xray.strategy;

import javax.servlet.http.HttpServletRequest;

import com.amazonaws.xray.entities.StringValidator;

public interface SegmentNamingStrategy {

    /**
     * Environment variable key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any system property, web.xml configuration value, or constructor value used for a fixed segment name.
     */
    public static final String NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_TRACING_NAME";

    /**
     * System property key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any web.xml configuration value or constructor value used for a fixed segment name.
     */
    public static final String NAME_OVERRIDE_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.strategy.tracingName";

    public String nameForRequest(HttpServletRequest request);

    default String getOverrideName() {
        String environmentNameOverrideValue = System.getenv(NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY);
        String systemNameOverrideValue = System.getProperty(NAME_OVERRIDE_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNotNullOrBlank(environmentNameOverrideValue)) {
            return environmentNameOverrideValue;
        } else if (StringValidator.isNotNullOrBlank(systemNameOverrideValue)) {
            return systemNameOverrideValue;
        }
        return null;
    }
}
