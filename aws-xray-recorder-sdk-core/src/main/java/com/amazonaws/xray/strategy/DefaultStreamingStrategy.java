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
import com.amazonaws.xray.entities.Subsegment;
import java.util.ArrayList;
import java.util.List;

public class DefaultStreamingStrategy implements StreamingStrategy {

    private static final int DEFAULT_MAX_SEGMENT_SIZE = 100;

    private final int maxSegmentSize;

    /**
     * Constructs an instance of DefaultStreamingStrategy using the default {@code maxSegmentSize} of 100.
     *
     */
    public DefaultStreamingStrategy() {
        this(DEFAULT_MAX_SEGMENT_SIZE);
    }

    /**
     * Constructs an instance of DefaultStreamingStrategy using the provided {@code maxSegmentSize}.
     *
     * @param maxSegmentSize
     *      the maximum number of subsegment nodes a segment tree may have before {@code requiresStreaming} will return true
     *
     * @throws IllegalArgumentException
     *      when {@code maxSegmentSize} is a negative integer
     *
     */
    public DefaultStreamingStrategy(int maxSegmentSize) {
        if (maxSegmentSize < 0) {
            throw new IllegalArgumentException("maxSegmentSize must be a non-negative integer.");
        }
        this.maxSegmentSize = maxSegmentSize;
    }

    public int getMaxSegmentSize() {
        return maxSegmentSize;
    }

    /**
     * {@inheritDoc}
     *
     * Indicates that the provided segment requires streaming when it has been marked for sampling and its tree of subsegments
     * reaches a size greater than {@code maxSegmentSize}.
     *
     * @see StreamingStrategy#requiresStreaming(Segment)
     */
    @Override
    public boolean requiresStreaming(Segment segment) {
        if (segment.isSampled()) {
            return segment.getTotalSize().intValue() > maxSegmentSize;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Performs Subtree Subsegment Streaming to stream completed subsegment subtrees. Serializes these subtrees of subsegments,
     * streams them to the daemon, and removes them from their parents.
     *
     * @see StreamingStrategy#streamSome(Entity,Emitter)
     */
    @Override
    public void streamSome(Entity entity, Emitter emitter) {
        stream(entity, emitter);
    }

    private boolean stream(Entity entity, Emitter emitter) {
        List<Subsegment> children = entity.getSubsegmentsCopy();
        List<Subsegment> streamable = new ArrayList<>();

        //Gather children and in the condition they are ready to stream, add them to the streamable list.
        if (children.size() > 0) {
            for (Subsegment child : children) {
                if (stream(child, emitter)) {
                    streamable.add(child);
                }
            }
        }

        //A subsegment is marked streamable if all of its children are streamable and the entity itself is not in progress.
        if (children.size() == streamable.size() && !entity.isInProgress()) {
            return true;
        }

        //Stream the subtrees that are ready.
        for (Subsegment child : streamable) {
            emitter.sendSubsegment(child);
            entity.removeSubsegment(child);
        }

        return false;
    }
}
