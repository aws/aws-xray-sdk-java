package com.amazonaws.xray.log4j;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.listeners.SegmentListener;
import org.apache.logging.log4j.ThreadContext;

/**
 * Implementation of SegmentListener that's used for Trace ID injection into logs that are recorded using the Log4J frontend API.
 *
 * To inject the X-Ray trace ID into your logging events, insert <code>%X{AWS-XRAY-TRACE-ID}</code> wherever you feel comfortable
 * in the layout pattern of your logging events. This is can be done wherever you configure Log4J for your project. Refer to the Log4J
 * <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html">configuration documentation</a> for more details.
 */
public class Log4JSegmentListener implements SegmentListener {
    private static final String TRACE_ID_KEY = "AWS-XRAY-TRACE-ID";

    /**
     * Maps the AWS-XRAY-TRACE-ID key to the trace ID of the segment that's just been created in the Log4J ThreadContext.
     *
     * @param oldEntity the previous entity or null
     * @param newEntity the new entity
     * The segment that has just begun
     */
    @Override
    public void onSetEntity(Entity oldEntity, Entity newEntity) {
        if (newEntity != null && newEntity.getTraceId() != null) {
            ThreadContext.put(TRACE_ID_KEY, TRACE_ID_KEY + ": " + newEntity.getTraceId().toString());
        }
    }

    /**
     * Removes the AWS-XRAY-TRACE-ID key from the ThreadContext upon the completion of each segment.
     *
     * @param entity
     * The segment that has just ended
     */
    @Override
    public void onClearEntity(Entity entity) {
        ThreadContext.remove(TRACE_ID_KEY);
    }
}
