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

package com.amazonaws.xray.listeners;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An interface to intercept lifecycle events, namely the beginning and ending, of segments produced by the AWSXRayRecorder.
 * Implementations should only contain cheap operations, since they'll be run very frequently.
 *
 */
public interface SegmentListener {

    /**
     * onBeginSegment is invoked immediately after a segment is created by the recorder.
     * The segment can be manipulated, e.g. with putAnnotation.
     *
     * @param segment
     * The segment that has just begun
     */
    default void onBeginSegment(Segment segment) {

    }

    /**
     * onBeginSubsegment is invoked immediately after a subsegment is created by the recorder.
     * The subsegment can be manipulated, e.g. with putAnnotation.
     *
     * @param subsegment
     * The subsegment that has just begun
     */
    default void onBeginSubsegment(Subsegment subsegment) {

    }

    /**
     * beforeEndSegment is invoked just before a segment is ended by the recorder.
     * The segment can be manipulated, e.g. with putAnnotation.
     *
     * @param segment
     * The segment that has just ended
     */
    default void beforeEndSegment(Segment segment) {

    }

    /**
     * afterEndSegment is invoked after a segment is ended by the recorder and emitted to the daemon.
     * The segment must not be modified since it has already been sent to X-Ray's backend.
     * Attempts to do so will raise an {@code AlreadyEmittedException}.
     *
     * @param segment
     * The segment that has just ended
     */
    default void afterEndSegment(Segment segment) {

    }

    /**
     * beforeEndSubsegment is invoked just before a subsegment is ended by the recorder.
     * The subsegment can be manipulated, e.g. with putAnnotation.
     *
     * @param subsegment
     * The subsegment that has just ended
     */
    default void beforeEndSubsegment(Subsegment subsegment) {

    }

    /**
     * afterEndSubsegment is invoked after a subsegment is ended by the recorder and emitted to the daemon.
     * The subsegment must not be modified since it has already been sent to X-Ray's backend.
     * Attempts to do so will raise an {@code AlreadyEmittedException}.
     *
     * @param subsegment
     * The subsegment that has just ended
     */
    default void afterEndSubsegment(Subsegment subsegment) {

    }

    /**
     * onSetEntity is invoked immediately before the SegmentContext is updated with a new entity.
     * Both the new entity and the previous entity (or null if unset) are passed.
     *
     * @param previousEntity
     * @param newEntity
     */
    default void onSetEntity(@Nullable Entity previousEntity, Entity newEntity) {

    }

    /**
     * onClearEntity is invoked just before the SegmentContext is cleared.
     *
     * @param previousEntity
     */
    default void onClearEntity(Entity previousEntity) {

    }

}
