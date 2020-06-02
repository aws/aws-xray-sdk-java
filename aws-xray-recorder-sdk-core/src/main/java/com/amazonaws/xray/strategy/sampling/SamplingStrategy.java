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

public interface SamplingStrategy {
    SamplingResponse shouldTrace(SamplingRequest sampleRequest);

    /**
     * Returns whether or not this sampling strategy supports 'forced sampling'.
     *
     * Forced sampling allows a segment's initial non-sampled decision to be later overriden to sampled. Supporting this feature
     * requires that all segments, sampled or otherwise, be kept in memory for the duration of their existence. Not supporting
     * this feature saves memory and computational capacity.
     *
     * @return whether or not forced sampling is supported
     */
    boolean isForcedSamplingSupported();

    /**
     * Shutdown additional resources created by advanced sampling strategies.
     */
    default void shutdown() {
    }
}
