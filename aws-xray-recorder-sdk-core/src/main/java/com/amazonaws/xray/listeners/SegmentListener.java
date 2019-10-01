package com.amazonaws.xray.listeners;

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
}