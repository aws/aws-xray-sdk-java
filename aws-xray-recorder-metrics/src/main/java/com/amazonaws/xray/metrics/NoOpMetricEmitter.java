package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.Segment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NoOpMetricEmitter implements MetricEmitter {

    private static final Log logger = LogFactory.getLog(NoOpMetricEmitter.class);

    @Override
    public void emitMetric(final Segment segment) {
        if(logger.isDebugEnabled()) {
            logger.debug("Not emitting metrics for Segment:" + segment.getTraceId() + segment.getId());
        }
    }
}
