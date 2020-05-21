package com.amazonaws.xray.strategy;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

public class DefaultStreamingStrategy implements StreamingStrategy {
    private static final Log logger = LogFactory.getLog(DefaultStreamingStrategy.class);

    private static final int DEFAULT_MAX_SEGMENT_SIZE = 100;

    private final int maxSegmentSize;

    public int getMaxSegmentSize() {
        return maxSegmentSize;
    }

    /**
     * {@inheritDoc}
     *
     * Constructs an instance of DefaultStreamingStrategy using the default {@code maxSegmentSize} of 100.
     *
     */
    public DefaultStreamingStrategy() { this(DEFAULT_MAX_SEGMENT_SIZE); }

    /**
     * {@inheritDoc}
     *
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

    /**
     * {@inheritDoc}
     *
     * Indicates that the provided segment requires streaming when it has been marked for sampling and its tree of subsegments reaches a size greater than {@code maxSegmentSize}.
     *
     * @see StreamingStrategy#requiresStreaming(Segment)
     */
    public boolean requiresStreaming(Segment segment) {
        if (segment.isSampled() && null != segment.getTotalSize()) {
            return segment.getTotalSize().intValue() > maxSegmentSize;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Performs Subtree Subsegment Streaming to stream completed subsegment subtrees. Serializes these subtrees of subsegments, streams them to the daemon, and removes them from their parents.
     *
     * @see StreamingStrategy#streamSome(Entity,Emitter)
     */

    public void streamSome(Entity entity, Emitter emitter) {
        if (entity.getSubsegmentsLock().tryLock()) {
            try {
                stream(entity, emitter);
            } finally {
                entity.getSubsegmentsLock().unlock();
            }
        }
    }

    private boolean stream(Entity entity, Emitter emitter) {
        ArrayList<Subsegment> children = new ArrayList<>(entity.getSubsegments());
        ArrayList<Subsegment> streamable = new ArrayList<>();

        //Gather children and in the condition they are ready to stream, add them to the streamable list.
        if (children.size() > 0) {
            for (Subsegment child : children) {
                if (child.getSubsegmentsLock().tryLock()) {
                    try {
                        if (stream(child, emitter)) {
                            streamable.add(child);
                        }
                    } finally {
                        child.getSubsegmentsLock().unlock();
                    }
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
            child.setEmitted(true);
            entity.removeSubsegment(child);
        }

        return false;
    }
}
