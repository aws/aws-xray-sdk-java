package com.amazonaws.xray.slf4j;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.listeners.SegmentListener;
import org.slf4j.MDC;

/**
 * Implementation of SegmentListener that's used for Trace ID injection into logs that are recorded using the SLF4J frontend API.
 *
 * To inject the X-Ray trace ID into your logging events, insert <code>%X{AWS-XRAY-TRACE-ID}</code> wherever you feel comfortable
 * in the <code>PatternLayout</code> of your logging events. For applications that use the Logback backend for SLF4J (the default implementation),
 * refer to the Logback <a href="https://logback.qos.ch/manual/layouts.html">layouts documentation</a> for how to do this. For applications using
 * Log4J as their backend, refer to the Log4j <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html">configuration documentation</a>.
 */
public class SLF4JSegmentListener implements SegmentListener {
    private static final String TRACE_ID_KEY = "AWS-XRAY-TRACE-ID";

    /**
     * Maps the AWS-XRAY-TRACE-ID key to the trace ID of the segment that's just been created in the MDC.
     *
     * @param segment
     * The segment that has just begun
     */
    @Override
    public void onBeginSegment(Segment segment) {

        // TODO: Check if we are in a Lambda environment (dealing w/ facade segment), and grab the Trace ID from the Header instead if so
        if (segment != null) {
            MDC.put(TRACE_ID_KEY, TRACE_ID_KEY + ": " + segment.getTraceId().toString());
        }
    }

    /**
     * Removes the AWS-XRAY-TRACE-ID key from the MDC upon the completion of each segment.
     *
     * @param segment
     * The segment that has just ended
     */
    @Override
    public void beforeEndSegment(Segment segment) {
        MDC.remove(TRACE_ID_KEY);
    }
}
