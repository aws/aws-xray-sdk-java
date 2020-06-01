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

public interface ContextMissingStrategy {

    /**
     * Environment variable key used to override the default {@code ContextMissingStrategy} used in new instances of
     * {@code AWSXRayRecorder}. Valid values for this environment variable are (case-insensitive) {@code RUNTIME_ERROR},
     * {@code LOG_ERROR} and {@code IGNORE_ERROR}. Invalid values will be ignored. Takes precedence over any system property or
     * builder value used for the {@code DefaultContextMissingStrategy}.
     */
    String CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY = "AWS_XRAY_CONTEXT_MISSING";

    /**
     * System property key used to override the default {@code ContextMissingStrategy} used in new instances of
     * {@code AWSXRayRecorder}. Valid values for this system property are (case-insensitive) {@code RUNTIME_ERROR},
     * {@code LOG_ERROR} and {@code IGNORE_ERROR}. Invalid values will be ignored. Takes precedence over any builder value used
     * for the {@code DefaultContextMissingStrategy}.
     */
    String CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY = "com.amazonaws.xray.strategy.contextMissingStrategy";

    void contextMissing(String message, Class<? extends RuntimeException> exceptionClass);
}
