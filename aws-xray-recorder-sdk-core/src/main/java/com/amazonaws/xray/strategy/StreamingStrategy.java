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

import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;

public interface StreamingStrategy {
    /**
     * Determines whether or not the provided segment requires any subsegment streaming.
     *
     * @param segment
     *            the segment to inspect
     * @return true if the segment should be streaming.
     */
    boolean requiresStreaming(Segment segment);


    /**
     * Streams (and removes) some subsegment children from the provided segment or subsegment.
     *
     * @param entity
     *            the segment or subsegment to stream children from
     * @param emitter
     *            the emitter to send the child subsegments to
     */
    void streamSome(Entity entity, Emitter emitter);
}
