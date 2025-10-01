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
import com.amazonaws.xray.entities.NoOpSegment;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;
import com.amazonaws.xray.listeners.SegmentListener;
import java.util.List;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.utilslite.SdkInternalThreadLocal;

public class LambdaSegmentContext implements SegmentContext {
    private static final Log logger = LogFactory.getLog(LambdaSegmentContext.class);

    private static final String LAMBDA_TRACE_HEADER_KEY = "_X_AMZN_TRACE_ID";
    private static final String CONCURRENT_TRACE_ID_KEY = "AWS_LAMBDA_X_TRACE_ID";

    // See: https://github.com/aws/aws-xray-sdk-java/issues/251
    private static final String LAMBDA_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";

    public static TraceHeader getTraceHeaderFromEnvironment() {
        String lambdaTraceHeaderKeyFromAwsSdkInternal = SdkInternalThreadLocal.get(CONCURRENT_TRACE_ID_KEY);
        String lambdaTraceHeaderKeyFromEnvVar = System.getenv(LAMBDA_TRACE_HEADER_KEY);

        if (lambdaTraceHeaderKeyFromAwsSdkInternal != null && lambdaTraceHeaderKeyFromAwsSdkInternal.length() > 0) {
            return TraceHeader.fromString(lambdaTraceHeaderKeyFromAwsSdkInternal);
        } else if (lambdaTraceHeaderKeyFromEnvVar != null && lambdaTraceHeaderKeyFromEnvVar.length() > 0) {
            return TraceHeader.fromString(lambdaTraceHeaderKeyFromEnvVar);
        } else {
            return TraceHeader.fromString(System.getProperty(LAMBDA_TRACE_HEADER_PROP));
        }
    }

    // SuppressWarnings is needed for passing Root TraceId to noOp segment
    @SuppressWarnings("nullness")
    @Override
    public Subsegment beginSubsegment(AWSXRayRecorder recorder, String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Beginning subsegment named: " + name);
        }

        TraceHeader traceHeader = LambdaSegmentContext.getTraceHeaderFromEnvironment();
        Entity entity = getTraceEntity();
        if (entity == null) { // First subsegment of a subsegment branch
            Segment parentSegment;
            // Trace header either takes the structure `Root=...;<extra-data>` or
            // `Root=...;Parent=...;Sampled=...;<extra-data>`
            if (traceHeader.getRootTraceId() != null && traceHeader.getParentId() != null && traceHeader.getSampled() != null) {
                parentSegment = new FacadeSegment(
                    recorder,
                    traceHeader.getRootTraceId(),
                    traceHeader.getParentId(),
                    traceHeader.getSampled());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating No-Op parent segment");
                }
                TraceID t = traceHeader.getRootTraceId() != null ? traceHeader.getRootTraceId() : TraceID.create(recorder);
                parentSegment = Segment.noOp(t, recorder);
            }

            boolean isRecording = parentSegment.isRecording();

            Subsegment subsegment = isRecording
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

            boolean isRecording = parentSubsegment.isRecording();

            Subsegment subsegment = isRecording
                    ? new SubsegmentImpl(recorder, name, parentSegment)
                    : Subsegment.noOp(parentSegment, recorder, name);
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
                if (((Subsegment) current).isSampled()) {
                    current.getCreator().getEmitter().sendSubsegment((Subsegment) current);
                }
                clearTraceEntity();
            } else if (parentEntity instanceof NoOpSegment) {
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
