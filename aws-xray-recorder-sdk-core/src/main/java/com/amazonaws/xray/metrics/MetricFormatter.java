package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.Segment;

/**
 * Convert a segment into a string representation of metrics
 */
public interface MetricFormatter {

    /**
     * Converts a segment into a metric string.
     * @param segment
     * @return
     */
    String formatSegment(Segment segment);

}
