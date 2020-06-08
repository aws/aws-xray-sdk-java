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
import com.amazonaws.xray.entities.FacadeSegment;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;
import com.amazonaws.xray.listeners.SegmentListener;
import java.util.List;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LambdaSegmentContext implements SegmentContext {
    private static final Log logger = LogFactory.getLog(LambdaSegmentContext.class);

    private static final String LAMBDA_TRACE_HEADER_KEY = "_X_AMZN_TRACE_ID";

    private static TraceHeader getTraceHeaderFromEnvironment() {
        return TraceHeader.fromString(System.getenv(LAMBDA_TRACE_HEADER_KEY));
    }

    private static boolean isInitializing(TraceHeader traceHeader) {
        return null == traceHeader.getRootTraceId() || null == traceHeader.getSampled() || null == traceHeader.getParentId();
    }

    private static FacadeSegment newFacadeSegment(AWSXRayRecorder recorder) {
        TraceHeader traceHeader = getTraceHeaderFromEnvironment();
        return new FacadeSegment(recorder, traceHeader.getRootTraceId(), traceHeader.getParentId(), traceHeader.getSampled());
    }

    @Override
    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning subsegment named: " + name);
        }
        if (null == getTraceEntity()) { // First subsgment of a subsegment branch.
            Segment parentSegment = null;
            if (LambdaSegmentContext.isInitializing(LambdaSegmentContext.getTraceHeaderFromEnvironment())) {
                logger.warn(LAMBDA_TRACE_HEADER_KEY + " is missing a trace ID, parent ID, or sampling decision. Subsegment "
                            + name + " discarded.");
                parentSegment = new FacadeSegment(recorder, TraceID.create(), "", SampleDecision.NOT_SAMPLED);
            } else {
                parentSegment = LambdaSegmentContext.newFacadeSegment(recorder);
            }
            Subsegment subsegment = new SubsegmentImpl(recorder, name, parentSegment);
            subsegment.setParent(parentSegment);
            // Enable FacadeSegment to keep track of its subsegments for subtree streaming
            parentSegment.addSubsegment(subsegment);
            setTraceEntity(subsegment);
            return subsegment;
        } else { // Continuation of a subsegment branch.
            Subsegment parentSubsegment = (Subsegment) getTraceEntity();
            // Ensure customers have not leaked subsegments across invocations
            TraceID environmentRootTraceId = LambdaSegmentContext.getTraceHeaderFromEnvironment().getRootTraceId();
            if (null != environmentRootTraceId &&
                !environmentRootTraceId.equals(parentSubsegment.getParentSegment().getTraceId())) {
                clearTraceEntity();
                return beginSubsegment(recorder, name);
            }
            Subsegment subsegment = new SubsegmentImpl(recorder, name, parentSubsegment.getParentSegment());
            subsegment.setParent(parentSubsegment);
            parentSubsegment.addSubsegment(subsegment);
            setTraceEntity(subsegment);

            List<SegmentListener> segmentListeners = recorder.getSegmentListeners();
            segmentListeners.stream()
                    .filter(Objects::nonNull)
                    .forEach(listener -> listener.onBeginSubsegment(subsegment));

            return subsegment;
        }
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

            currentSubsegment.end();

            if (recorder.getStreamingStrategy().requiresStreaming(currentSubsegment.getParentSegment())) {
                recorder.getStreamingStrategy().streamSome(currentSubsegment.getParentSegment(), recorder.getEmitter());
            }

            segmentListeners
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(listener -> listener.afterEndSubsegment(currentSubsegment));

            Entity parentEntity = current.getParent();
            if (parentEntity instanceof FacadeSegment) {
                if (((FacadeSegment) parentEntity).isSampled()) {
                    current.getCreator().getEmitter().sendSubsegment((Subsegment) current);
                }
                clearTraceEntity();
            } else {
                setTraceEntity(current.getParent());
            }

        } else {
            throw new SubsegmentNotFoundException("Failed to end a subsegment: subsegment cannot be found.");
        }
    }
}
