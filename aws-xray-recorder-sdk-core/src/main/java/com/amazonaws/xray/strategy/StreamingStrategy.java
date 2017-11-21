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
    public boolean requiresStreaming(Segment segment);


    /**
     * Streams (and removes) some subsegment children from the provided segment or subsegment.
     *
     * @param entity
     *            the segment or subsegment to stream children from
     * @param emitter
     *            the emitter to send the child subsegments to
     */
    public void streamSome(Entity entity, Emitter emitter);
}
