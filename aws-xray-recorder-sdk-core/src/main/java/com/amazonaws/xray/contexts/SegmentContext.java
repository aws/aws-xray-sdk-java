package com.amazonaws.xray.contexts;

import java.util.Objects;
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
        if(entity != null && entity.getCreator() != null) {
            entity.getCreator().getSegmentListeners().stream().filter(Objects::nonNull).forEach(l -> {
                l.onSetEntity(ThreadLocalStorage.get(), entity);
            });
        }
        ThreadLocalStorage.set(entity);
    }

    default void clearTraceEntity() {
        Entity oldEntity = ThreadLocalStorage.get();
        if(oldEntity != null && oldEntity.getCreator() != null)
        oldEntity.getCreator().getSegmentListeners().stream().filter(Objects::nonNull).forEach(l -> {
            l.onClearEntity(oldEntity);
        });
        ThreadLocalStorage.clear();
    }

    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name);

    public void endSubsegment(AWSXRayRecorder recorder);
}
