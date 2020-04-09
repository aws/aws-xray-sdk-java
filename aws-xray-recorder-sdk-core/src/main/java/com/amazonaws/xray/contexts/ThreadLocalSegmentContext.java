package com.amazonaws.xray.contexts;

import com.amazonaws.xray.listeners.SegmentListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;

import java.util.List;
import java.util.Objects;

public class ThreadLocalSegmentContext implements SegmentContext {
    private static final Log logger =
        LogFactory.getLog(ThreadLocalSegmentContext.class);


    @Override
    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name) {
        Entity current = getTraceEntity();
        if (null == current) {
            recorder.getContextMissingStrategy().contextMissing("Failed to begin subsegment named '" + name + "': segment cannot be found.", SegmentNotFoundException.class);
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning subsegment named: " + name);
        }
        Segment parentSegment = getTraceEntity().getParentSegment();
        Subsegment subsegment = new SubsegmentImpl(recorder, name, parentSegment);
        subsegment.setParent(current);
        current.addSubsegment(subsegment);
        setTraceEntity(subsegment);

        List<SegmentListener> segmentListeners = recorder.getSegmentListeners();
        segmentListeners.stream()
                .filter(Objects::nonNull)
                .forEach(listener -> listener.onBeginSubsegment(subsegment));

        return subsegment;
    }

    @Override
    public void endSubsegment(AWSXRayRecorder recorder) {
        Entity current = getTraceEntity();
        if (current instanceof Subsegment) {
            if (logger.isDebugEnabled()) {
                logger.debug("Ending subsegment named: " + current.getName());
            }
            Subsegment currentSubsegment = (Subsegment) current;

            List<SegmentListener> segmentListeners = recorder.getSegmentListeners();
            segmentListeners
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(listener -> listener.beforeEndSubsegment(currentSubsegment));

            if (currentSubsegment.end()) {
                recorder.sendSegment(currentSubsegment.getParentSegment());
            } else {
                if (recorder.getStreamingStrategy().requiresStreaming(currentSubsegment.getParentSegment())) {
                    recorder.getStreamingStrategy().streamSome(currentSubsegment.getParentSegment(), recorder.getEmitter());
                }

                segmentListeners
                        .stream()
                        .filter(Objects::nonNull)
                        .forEach(listener -> listener.afterEndSubsegment(currentSubsegment));

                setTraceEntity(current.getParent());
            }
        } else {
            recorder.getContextMissingStrategy().contextMissing("Failed to end subsegment: subsegment cannot be found.", SubsegmentNotFoundException.class);
        }
    }
}
