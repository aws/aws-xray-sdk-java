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

import com.amazonaws.xray.entities.StringValidator;
import javax.servlet.http.HttpServletRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface SegmentNamingStrategy {

    /**
     * Environment variable key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any system property, web.xml configuration value, or constructor value used for a fixed segment name.
     */
    String NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_TRACING_NAME";

    /**
     * System property key used to override the default segment name used by implementors of {@code SegmentNamingStrategy}.
     * Takes precedence over any web.xml configuration value or constructor value used for a fixed segment name.
     */
    String NAME_OVERRIDE_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.strategy.tracingName";

    /**
     * Returns a {@link SegmentNamingStrategy} that assigns the provided {@code name} to all segments generated for incoming
     * requests. This will be ignored and will use the the value of the {@code AWS_XRAY_TRACING_NAME} environment variable or
     * {@code com.amazonaws.xray.strategy.tracingName} system property if set.
     */
    static SegmentNamingStrategy fixed(String name) {
        return new FixedSegmentNamingStrategy(name);
    }

    /**
     * Returns a {@link SegmentNamingStrategy} that names segments based on the {@code Host} header of incoming requests,
     * accepting any {@code Host} header value.
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request or the incoming host header value
     *  does not match the provided pattern. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment
     *  variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     */
    static SegmentNamingStrategy dynamic(String fallbackName) {
        return dynamic(fallbackName, "*");
    }

    /**
     * Returns a {@link SegmentNamingStrategy} that names segments based on the {@code Host} header of incoming requests,
     * accepting only recognized {@code Host} header values.
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request or the incoming host header value
     *  does not match the provided pattern. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment
     *  variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     * @param recognizedHosts
     *  the pattern to match the incoming host header value against. This pattern is compared against the incoming host header
     *  using the {@link com.amazonaws.xray.entities.SearchPattern#wildcardMatch(String, String)} method.
     */
    static SegmentNamingStrategy dynamic(String fallbackName, String recognizedHosts) {
        return new DynamicSegmentNamingStrategy(fallbackName, recognizedHosts);
    }

    String nameForRequest(HttpServletRequest request);

    @Nullable
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
