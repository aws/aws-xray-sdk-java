package com.amazonaws.xray.contexts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.ThreadLocalStorage;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

public interface SegmentContext {
    static final Log logger =
        LogFactory.getLog(SegmentContext.class);

    default Segment beginSegment(AWSXRayRecorder recorder, Segment segment) {
        return segment;
    }

    default void endSegment(AWSXRayRecorder recorder) {
    }

    default Entity getTraceEntity() {
        return ThreadLocalStorage.get();
    }

    default void setTraceEntity(Entity entity) {
        ThreadLocalStorage.set(entity);
    }

    default void clearTraceEntity() {
        ThreadLocalStorage.clear();
    }

    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name);

    public void endSubsegment(AWSXRayRecorder recorder);
}
