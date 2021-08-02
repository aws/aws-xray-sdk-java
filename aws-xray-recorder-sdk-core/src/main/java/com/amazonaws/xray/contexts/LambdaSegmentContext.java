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
    
    // See: https://github.com/aws/aws-xray-sdk-java/issues/251
    private static final String LAMBDA_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";

    private static TraceHeader getTraceHeaderFromEnvironment() {
        String lambdaTraceHeaderKey = System.getenv(LAMBDA_TRACE_HEADER_KEY);
        return TraceHeader.fromString(lambdaTraceHeaderKey != null && lambdaTraceHeaderKey.length() > 0 
            ? lambdaTraceHeaderKey 
            : System.getProperty(LAMBDA_TRACE_HEADER_PROP));
    }

    private static boolean isInitializing(TraceHeader traceHeader) {
        return traceHeader.getRootTraceId() == null || traceHeader.getSampled() == null || traceHeader.getParentId() == null;
    }

    private static FacadeSegment newFacadeSegment(AWSXRayRecorder recorder, String name) {
        TraceHeader traceHeader = getTraceHeaderFromEnvironment();
        if (isInitializing(traceHeader)) {
            logger.warn(LAMBDA_TRACE_HEADER_KEY + " is missing a trace ID, parent ID, or sampling decision. Subsegment "
                        + name + " discarded.");
            return new FacadeSegment(recorder, TraceID.create(recorder), "", SampleDecision.NOT_SAMPLED);
        }
        return new FacadeSegment(recorder, traceHeader.getRootTraceId(), traceHeader.getParentId(), traceHeader.getSampled());
    }

    @Override
    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning subsegment named: " + name);
        }
        Entity entity = getTraceEntity();
        if (entity == null) { // First subsgment of a subsegment branch.
            Segment parentSegment = newFacadeSegment(recorder, name);
            Subsegment subsegment = parentSegment.isRecording()
                                    ? new SubsegmentImpl(recorder, name, parentSegment)
                                    : Subsegment.noOp(parentSegment, recorder);
            subsegment.setParent(parentSegment);
            // Enable FacadeSegment to keep track of its subsegments for subtree streaming
            parentSegment.addSubsegment(subsegment);
            setTraceEntity(subsegment);
            return subsegment;
        } else { // Continuation of a subsegment branch.
            Subsegment parentSubsegment = (Subsegment) entity;
            // Ensure customers have not leaked subsegments across invocations
            TraceID environmentRootTraceId = LambdaSegmentContext.getTraceHeaderFromEnvironment().getRootTraceId();
            if (environmentRootTraceId != null &&
                !environmentRootTraceId.equals(parentSubsegment.getParentSegment().getTraceId())) {
                clearTraceEntity();
                return beginSubsegment(recorder, name);
            }
            Segment parentSegment = parentSubsegment.getParentSegment();
            Subsegment subsegment = parentSegment.isRecording()
                                    ? new SubsegmentImpl(recorder, name, parentSegment)
                                    : Subsegment.noOp(parentSegment, recorder);
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
            recorder.getContextMissingStrategy().contextMissing("Failed to end subsegment: subsegment cannot be found.",
                SubsegmentNotFoundException.class);
        }
    }
}
