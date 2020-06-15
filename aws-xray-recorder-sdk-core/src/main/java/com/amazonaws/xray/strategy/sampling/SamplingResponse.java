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

package com.amazonaws.xray.strategy.sampling;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the sampling decision output by the sampler. Used by the SDK to
 * decide whether or not a segment should be emitted.
 */
public class SamplingResponse {

    private boolean sampled;

    @Nullable
    private String ruleName;

    /**
     * @param sampled
     *            the boolean sampling decision
     * @param ruleName
     *            the name of the rule used to make the sampling decision
     */
    public SamplingResponse(boolean sampled, String ruleName) {
        this.sampled = sampled;
        this.ruleName = ruleName;
    }

    public SamplingResponse(String ruleName) {
        this.ruleName = ruleName;
    }

    public SamplingResponse(boolean sampled) {
        this.sampled = sampled;
    }

    public SamplingResponse() {
    }

    public boolean isSampled() {
        return sampled;
    }

    public Optional<String> getRuleName() {
        return Optional.ofNullable(ruleName);
    }

    public SamplingResponse setSampled(boolean sampled) {
        this.sampled = sampled;
        return this;
    }

}
