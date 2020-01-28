package com.amazonaws.xray.listeners;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;

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
     * beforeEndSegment is invoked just before a segment is ended by the recorder.
     * The segment can be manipulated, e.g. with putAnnotation.
     *
     * @param segment
     * The segment that has just ended
     */
    default void beforeEndSegment(Segment segment) {

    }

    /**
     * afterEndSegment is invoked after a segment is ended by the recorder and immediately before it is emitted to the daemon.
     * The segment must not be manipulated. Attempts to do so will raise an exception.
     *
     * @param segment
     * The segment that has just ended
     */
    default void afterEndSegment(Segment segment) {

    }

    /**
     * onSetEntity is invoked when the SegmentContext is being updated with a new entity.
     * Both the new entity and the previous entity (or null if unset) are passed.
     *
     * @param previousEntity
     * @param newEntity
     */
    default void onSetEntity(Entity previousEntity, Entity newEntity) {

    }

    /**
     * onClearEntity is invoked just before the SegmentContext is cleared.
     *
     * @param previousEntity
     */
    default void onClearEntity(Entity previousEntity) {

    }

}