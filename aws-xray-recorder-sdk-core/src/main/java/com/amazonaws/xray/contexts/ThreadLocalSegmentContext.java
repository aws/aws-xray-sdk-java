/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.contexts;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;
import com.amazonaws.xray.listeners.SegmentListener;
import java.util.List;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThreadLocalSegmentContext implements SegmentContext {
    private static final Log logger =
        LogFactory.getLog(ThreadLocalSegmentContext.class);


    @Override
    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name) {
        Entity current = getTraceEntity();
        if (current == null) {
            recorder.getContextMissingStrategy().contextMissing("Failed to begin subsegment named '" + name
                                                                + "': segment cannot be found.", SegmentNotFoundException.class);
            return Subsegment.noOp(recorder, false);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning subsegment named: " + name);
        }
        Segment parentSegment = current.getParentSegment();
        Subsegment subsegment = parentSegment.isRecording()
                                ? new SubsegmentImpl(recorder, name, parentSegment)
                                : Subsegment.noOp(parentSegment, recorder);
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
                if (current.getName().isEmpty() && !current.getParentSegment().isSampled()) {
                    logger.debug("Ending no-op subsegment");
                } else {
                    logger.debug("Ending subsegment named: " + current.getName());
                }
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
            recorder.getContextMissingStrategy().contextMissing("Failed to end subsegment: subsegment cannot be found.",
                                                                SubsegmentNotFoundException.class);
        }
    }
}
