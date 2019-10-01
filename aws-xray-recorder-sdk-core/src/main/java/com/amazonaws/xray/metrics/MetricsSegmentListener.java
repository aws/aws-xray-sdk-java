package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.FacadeSegment;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.listeners.SegmentListener;

public class MetricsSegmentListener implements SegmentListener {

    MetricEmitter emitter;

    //TODO: Use the correct emitter if we're running in Lambda vs w/ CW Agent
    public MetricsSegmentListener() {
      emitter = new StdoutMetricEmitter();
    }

    @Override
    public void afterEndSegment(final Segment segment) {
        if(segment != null && !(segment instanceof FacadeSegment)) {
            emitter.emitMetric(segment);
        }
    }
}
