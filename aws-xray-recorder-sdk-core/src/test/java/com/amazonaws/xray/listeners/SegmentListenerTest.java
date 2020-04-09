package com.amazonaws.xray.listeners;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SegmentListenerTest {
    class CustomSegmentListener implements SegmentListener {

        @Override
        public void onBeginSegment(Segment segment) {
            segment.putAnnotation("beginTest", "isPresent");
        }

        @Override
        public void onBeginSubsegment(Subsegment subsegment) { subsegment.putAnnotation("subAnnotation1", "began"); }

        @Override
        public void beforeEndSegment(Segment segment) { segment.putAnnotation("endTest", "isPresent"); }

        @Override
        public void beforeEndSubsegment(Subsegment subsegment) { subsegment.putAnnotation("subAnnotation2", "ended"); }
    }

    class SecondSegmentListener implements SegmentListener {
        private int testVal = 0;
        private int testVal2 = 0;

        @Override
        public void onBeginSegment(Segment segment) { testVal = 1; }

        @Override
        public void beforeEndSegment(Segment segment) {
            testVal2 = 1;
        }

        public int getTestVal() {
            return testVal;
        }
        public int getTestVal2() { return testVal2; }
    }

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.any());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.any());
        CustomSegmentListener segmentListener = new CustomSegmentListener();

        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSegmentListener(segmentListener).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testOnBeginSegment() {
        Segment test = AWSXRay.beginSegment("test");
        String beginAnnotation = test.getAnnotations().get("beginTest").toString();

        Assert.assertEquals("isPresent", beginAnnotation);

        AWSXRay.endSegment();
    }

    @Test
    public void testOnEndSegment() {
        Segment test = AWSXRay.beginSegment("test");
        AWSXRay.endSegment();
        String endAnnotation = test.getAnnotations().get("endTest").toString();

        Assert.assertEquals("isPresent", endAnnotation);
    }

    @Test
    public void testSubsegmentListeners() {
        AWSXRay.beginSegment("test");
        Subsegment sub = AWSXRay.beginSubsegment("testSub");
        String beginAnnotation = sub.getAnnotations().get("subAnnotation1").toString();
        AWSXRay.endSubsegment();
        String endAnnotation = sub.getAnnotations().get("subAnnotation2").toString();

        Assert.assertEquals("began", beginAnnotation);
        Assert.assertEquals("ended", endAnnotation);
    }

    @Test
    public void testMultipleSegmentListeners() {
        SecondSegmentListener secondSegmentListener = new SecondSegmentListener();
        AWSXRay.getGlobalRecorder().addSegmentListener(secondSegmentListener);
        Segment test = AWSXRay.beginSegment("test");
        String beginAnnotation = test.getAnnotations().get("beginTest").toString();


        Assert.assertEquals(1, secondSegmentListener.getTestVal());

        Assert.assertEquals("isPresent", beginAnnotation);

        AWSXRay.endSegment();
        String endAnnotation = test.getAnnotations().get("endTest").toString();

        Assert.assertEquals("isPresent", endAnnotation);
        Assert.assertEquals(1, secondSegmentListener.getTestVal2());
    }
}
