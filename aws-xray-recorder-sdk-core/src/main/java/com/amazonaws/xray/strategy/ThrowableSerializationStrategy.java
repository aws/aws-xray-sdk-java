package com.amazonaws.xray.strategy;

import java.util.List;

import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.ThrowableDescription;

public interface ThrowableSerializationStrategy {
    /**
     * Serializes a {@code Throwable} into a {@code ThrowableDescription}. Uses the provided subsegments to chain exceptions where possible.
     *
     * @param throwable
     *            the Throwable to serialize
     * @param subsegments
     *            the list of subsegment children in which to look for the same {@code Throwable} object, for chaining
     *
     * @return a list of {@code ThrowableDescription}s which represent the provided {@code Throwable}
     */
    public List<ThrowableDescription> describeInContext(Throwable throwable, List<Subsegment> subsegments);
}
