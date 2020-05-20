package com.amazonaws.xray.emitters;

import static java.util.Objects.requireNonNull;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

/**
 * An {@link Emitter} which delegates all calls to another {@link Emitter}.
 * Extend from this class to customize when segments and subsegments are sent.
 *
 * <p>For example, {@code
 * class CircuitBreakingEmitter extends DelegatingEmitter {
 *
 *     private final CircuitBreaker circuitBreaker;
 *
 *     CircuitBreakingEmitter() {
 *         super(Emitter.create());
 *         circuitBreaker = CircuitBreaker.create();
 *     }
 *
 *     @Override
 *     public boolean sendSegment(Segment segment) {
 *         if (circuitBreaker.isOpen()) {
 *             return super.sendSegment(segment);
 *         }
 *         return false;
 *     }
 * }
 * }
 */
public class DelegatingEmitter extends Emitter {

    private final Emitter delegate;

    protected DelegatingEmitter(Emitter delegate) {
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public boolean sendSegment(Segment segment) {
        return delegate.sendSegment(segment);
    }

    @Override
    public boolean sendSubsegment(Subsegment subsegment) {
        return delegate.sendSubsegment(subsegment);
    }
}
