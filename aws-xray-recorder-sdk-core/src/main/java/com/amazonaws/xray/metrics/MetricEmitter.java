package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.Segment;

/**
 * Extract metrics from a segment and emit them to a a given destination (e.g. EMF via stdout for Lambda or EMF via UDP for containers or EC2).
 */
public interface MetricEmitter {

    /**
     * Format the given metrics and emit it.
     */
    void emitMetric(final Segment segment);

}
