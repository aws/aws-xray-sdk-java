package com.amazonaws.xray.slf4j;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.StringValidator;
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
    private String prefix;

    /**
     * Returns a default {@code SLF4JSegmentListener} using {@code AWS-XRAY-TRACE-ID} as the prefix for trace ID injections
     */
    public SLF4JSegmentListener() {
        this(TRACE_ID_KEY);
    }

    /**
     * Returns a {@code SLF4JSegmentListener} using the provided prefix for trace ID injections
     * @param prefix
     */
    public SLF4JSegmentListener(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the trace ID injection prefix to provided value. A colon and space separate the prefix and trace ID, unless
     * the {@code prefix} is null or blank.
     * @param prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Maps the AWS-XRAY-TRACE-ID key to the trace ID of the entity that's just been created in the MDC.
     * Does not perform injection if entity is not available or not sampled, since then the given entity would not be displayed
     * in X-Ray console.
     *
     * @param oldEntity the previous entity or null
     * @param newEntity the new entity, either a subsegment or segment
     */
    @Override
    public void onSetEntity(Entity oldEntity, Entity newEntity) {
        if (newEntity == null) {
            MDC.remove(TRACE_ID_KEY);
            return;
        }

        Segment segment =  newEntity instanceof Segment ? ((Segment) newEntity) : newEntity.getParentSegment();

        if (segment != null && segment.getTraceId() != null && segment.isSampled() && newEntity.getId() != null) {
            String fullPrefix = StringValidator.isNullOrBlank(this.prefix) ? "" : this.prefix + ": ";
            MDC.put(TRACE_ID_KEY, fullPrefix + segment.getTraceId() + "@" + newEntity.getId());
        } else {
            MDC.remove(TRACE_ID_KEY);  // Ensure traces don't spill over to unlinked messages
        }
    }

    /**
     * Removes the AWS-XRAY-TRACE-ID key from the MDC upon the completion of each segment.
     *
     * @param entity
     * The segment that has just ended
     */
    @Override
    public void onClearEntity(Entity entity) {
        MDC.remove(TRACE_ID_KEY);
    }
}
