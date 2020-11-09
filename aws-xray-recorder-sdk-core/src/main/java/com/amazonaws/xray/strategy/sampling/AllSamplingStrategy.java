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

/**
 * A sampling strategy for which {@code shouldTrace} always returns true. Use this sampling strategy to trace every request.
 *
 */
public class AllSamplingStrategy implements SamplingStrategy {
    @Override
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        SamplingResponse sampleResponse = new SamplingResponse(true);
        return sampleResponse;
    }

    @Override
    public boolean isForcedSamplingSupported() {
        return false;
    }
}
