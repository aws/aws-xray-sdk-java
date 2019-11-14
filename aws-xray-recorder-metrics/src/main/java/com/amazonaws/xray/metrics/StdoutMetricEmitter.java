package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.Segment;

/**
 * Writes EMF formatted structured logs to stdout for testing.
 */
public class StdoutMetricEmitter implements MetricEmitter {

    private MetricFormatter formatter;

    public StdoutMetricEmitter() {
        formatter = new EMFMetricFormatter();
    }

    @Override
    public void emitMetric(final Segment segment) {
        String output = formatter.formatSegment(segment);
        System.out.println(output);
    }
}
