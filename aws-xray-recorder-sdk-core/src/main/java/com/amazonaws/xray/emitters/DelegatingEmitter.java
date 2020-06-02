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

package com.amazonaws.xray.emitters;

import static java.util.Objects.requireNonNull;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

/**
 * An {@link Emitter} which delegates all calls to another {@link Emitter}.
 * Extend from this class to customize when segments and subsegments are sent.
 *
 * <pre>{@code
 * class CircuitBreakingEmitter extends DelegatingEmitter {
 *
 *     private final CircuitBreaker circuitBreaker;
 *
 *     CircuitBreakingEmitter() {
 *         super(Emitter.create());
 *         circuitBreaker = CircuitBreaker.create();
 *     }
 *
 *     {@literal @}Override
 *     public boolean sendSegment(Segment segment) {
 *         if (circuitBreaker.isOpen()) {
 *             return super.sendSegment(segment);
 *         }
 *         return false;
 *     }
 * }
 * }</pre>
 */
public class DelegatingEmitter extends Emitter {

    private final Emitter delegate;

    /**
     * Constructs a new {@link DelegatingEmitter} that delegates all calls to the provided {@link Emitter}.
     */
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
