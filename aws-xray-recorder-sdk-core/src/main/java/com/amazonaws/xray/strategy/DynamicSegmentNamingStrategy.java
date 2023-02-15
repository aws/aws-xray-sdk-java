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

import com.amazonaws.xray.entities.SearchPattern;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

/**
 * @deprecated Use {@link SegmentNamingStrategy#dynamic(String)}.
 */
@Deprecated
public class DynamicSegmentNamingStrategy extends com.amazonaws.xray.strategy.interfaces.DynamicSegmentNamingStrategy implements com.amazonaws.xray.strategy.SegmentNamingStrategy {
    /**
     * Creates an instance of {@code DynamicSegmentNamingStrategy} with the provided {@code fallbackName} and a
     * {@code recognizedHosts} value of "*".
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request. This will be overriden by the
     *  value of the {@code AWS_XRAY_TRACING_NAME} environment variable or {@code com.amazonaws.xray.strategy.tracingName} system
     *  property, if either are set to a non-empty value.
     *
     * @deprecated Use {@link com.amazonaws.xray.strategy.jakarta.SegmentNamingStrategy#dynamic(String)}.
     */
    @Deprecated
    public DynamicSegmentNamingStrategy(String fallbackName) {
        super(fallbackName, "*");
    }

    /**
     * Creates an instance of {@code DynamicSegmentNamingStrategy} with the provided {@code fallbackName} and
     * {@code recognizedHosts} values.
     *
     * @param fallbackName
     *  the fallback segment name used when no host header is included in the incoming request or the incoming host header value
     *  does not match the provided pattern. This will be overriden by the value of the {@code AWS_XRAY_TRACING_NAME} environment
     *  variable or {@code com.amazonaws.xray.strategy.tracingName} system property, if either are set to a non-empty value.
     * @param recognizedHosts
     *  the pattern to match the incoming host header value against. This pattern is compared against the incoming host header
     *  using the {@link SearchPattern#wildcardMatch(String, String)} method.
     *
     * @deprecated Use {@link com.amazonaws.xray.strategy.jakarta.SegmentNamingStrategy#dynamic(String, String)}.
     */
    // Instance method is called before the class is initialized. This can cause undefined behavior, e.g., if getOverrideName
    // accesses fallbackName. This class doesn't really need to be exposed to users so we suppress for now and will clean up after
    // hiding.
    @SuppressWarnings("nullness")
    @Deprecated
    public DynamicSegmentNamingStrategy(String fallbackName, String recognizedHosts) {
        super(fallbackName, recognizedHosts);
    }

    /**
     *
     * Returns the derived segment name for an incoming request. Attempts to get the {@code Host} header from the
     * {@code HttpServletRequest}. If the {@code Host} header has a value and if the value matches the optionally provided
     * {@code recognizedHosts} pattern, then this value is returned as the segment name. Otherwise, {@code fallbackName} is
     * returned.
     *
     *
     * @param request
     *  the incoming request
     * @return
     *  the segment name for the incoming request.
     */
    @Override
    public String nameForRequest(HttpServletRequest request) {
        Optional<String> hostHeaderValue = Optional.ofNullable(request.getHeader("Host"));
        if (hostHeaderValue.isPresent() &&
                (null == recognizedHosts || SearchPattern.wildcardMatch(recognizedHosts, hostHeaderValue.get()))) {
            return hostHeaderValue.get();
        }
        return fallbackName;
    }
}
