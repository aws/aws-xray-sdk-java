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

package com.amazonaws.xray.listeners;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SegmentListenerTest {
    static class CustomSegmentListener implements SegmentListener {

        @Override
        public void onBeginSegment(Segment segment) {
            segment.putAnnotation("beginTest", "isPresent");
        }

        @Override
        public void onBeginSubsegment(Subsegment subsegment) {
            subsegment.putAnnotation("subAnnotation1", "began");
        }

        @Override
        public void beforeEndSegment(Segment segment) {
            segment.putAnnotation("endTest", "isPresent");
        }

        @Override
        public void beforeEndSubsegment(Subsegment subsegment) {
            subsegment.putAnnotation("subAnnotation2", "ended");
        }
    }

    static class SecondSegmentListener implements SegmentListener {
        private int testVal = 0;
        private int testVal2 = 0;

        @Override
        public void onBeginSegment(Segment segment) {
            testVal = 1;
        }

        @Override
        public void beforeEndSegment(Segment segment) {
            testVal2 = 1;
        }

        int getTestVal() {
            return testVal;
        }

        int getTestVal2() {
            return testVal2;
        }
    }

    @BeforeEach
    void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.any());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.any());
        CustomSegmentListener segmentListener = new CustomSegmentListener();

        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard()
                                                        .withEmitter(blankEmitter)
                                                        .withSegmentListener(segmentListener)
                                                        .build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testOnBeginSegment() {
        Segment test = AWSXRay.beginSegment("test");
        String beginAnnotation = test.getAnnotations().get("beginTest").toString();

        Assertions.assertEquals("isPresent", beginAnnotation);

        AWSXRay.endSegment();
    }

    @Test
    void testOnEndSegment() {
        Segment test = AWSXRay.beginSegment("test");
        AWSXRay.endSegment();
        String endAnnotation = test.getAnnotations().get("endTest").toString();

        Assertions.assertEquals("isPresent", endAnnotation);
    }

    @Test
    void testSubsegmentListeners() {
        AWSXRay.beginSegment("test");
        Subsegment sub = AWSXRay.beginSubsegment("testSub");
        String beginAnnotation = sub.getAnnotations().get("subAnnotation1").toString();
        AWSXRay.endSubsegment();
        String endAnnotation = sub.getAnnotations().get("subAnnotation2").toString();

        Assertions.assertEquals("began", beginAnnotation);
        Assertions.assertEquals("ended", endAnnotation);
    }

    @Test
    void testMultipleSegmentListeners() {
        SecondSegmentListener secondSegmentListener = new SecondSegmentListener();
        AWSXRay.getGlobalRecorder().addSegmentListener(secondSegmentListener);
        Segment test = AWSXRay.beginSegment("test");
        String beginAnnotation = test.getAnnotations().get("beginTest").toString();


        Assertions.assertEquals(1, secondSegmentListener.getTestVal());

        Assertions.assertEquals("isPresent", beginAnnotation);

        AWSXRay.endSegment();
        String endAnnotation = test.getAnnotations().get("endTest").toString();

        Assertions.assertEquals("isPresent", endAnnotation);
        Assertions.assertEquals(1, secondSegmentListener.getTestVal2());
    }
}
