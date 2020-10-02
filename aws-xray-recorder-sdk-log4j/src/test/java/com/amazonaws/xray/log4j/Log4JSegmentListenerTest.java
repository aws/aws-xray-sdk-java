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

package com.amazonaws.xray.log4j;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceID;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class Log4JSegmentListenerTest {
    private static final TraceID TRACE_ID =  TraceID.fromString("1-1-d0a73661177562839f503b9f");
    private static final String TRACE_ID_KEY = "AWS-XRAY-TRACE-ID";

    @BeforeEach
    void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.any());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.any());
        Log4JSegmentListener segmentListener = new Log4JSegmentListener();

        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard()
                                                        .withEmitter(blankEmitter)
                                                        .withSegmentListener(segmentListener)
                                                        .build());
        AWSXRay.clearTraceEntity();
        ThreadContext.clearAll();
    }

    @Test
    void testDefaultPrefix() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);

        listener.onSetEntity(null, seg);

        Assertions.assertEquals(TRACE_ID_KEY + ": " + TRACE_ID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    void testSetPrefix() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);

        listener.onSetEntity(null, seg);

        Assertions.assertEquals(TRACE_ID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    void testSegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);
        listener.onSetEntity(null, seg);

        Assertions.assertEquals(TRACE_ID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    void testUnsampledSegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);
        seg.setSampled(false);
        listener.onSetEntity(null, seg);

        Assertions.assertNull(ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    void testSubsegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);
        listener.onSetEntity(null, seg);
        Subsegment sub = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", seg);
        listener.onSetEntity(seg, sub);

        Assertions.assertEquals(TRACE_ID.toString() + "@" + sub.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    void testNestedSubsegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", TRACE_ID);
        listener.onSetEntity(null, seg);
        Subsegment sub1 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test1", seg);
        listener.onSetEntity(seg, sub1);
        Subsegment sub2 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test2", seg);
        listener.onSetEntity(sub1, sub2);

        Assertions.assertEquals(TRACE_ID.toString() + "@" + sub2.getId(), ThreadContext.get(TRACE_ID_KEY));
    }
}
