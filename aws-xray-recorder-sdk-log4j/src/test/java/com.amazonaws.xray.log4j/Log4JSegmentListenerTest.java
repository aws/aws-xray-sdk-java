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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class Log4JSegmentListenerTest {
    private final TraceID traceID =  TraceID.fromString("1-1-d0a73661177562839f503b9f");
    private static final String TRACE_ID_KEY = "AWS-XRAY-TRACE-ID";

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.any());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.any());
        Log4JSegmentListener segmentListener = new Log4JSegmentListener();

        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSegmentListener(segmentListener).build());
        AWSXRay.clearTraceEntity();
        ThreadContext.clearAll();
    }

    @Test
    public void testDefaultPrefix() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);

        listener.onSetEntity(null, seg);

        Assert.assertEquals(TRACE_ID_KEY + ": " + traceID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    public void testSetPrefix() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);

        listener.onSetEntity(null, seg);

        Assert.assertEquals(traceID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    public void testSegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);
        listener.onSetEntity(null, seg);

        Assert.assertEquals(traceID.toString() + "@" + seg.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    public void testUnsampledSegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);
        seg.setSampled(false);
        listener.onSetEntity(null, seg);

        Assert.assertNull(ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    public void testSubsegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);
        listener.onSetEntity(null, seg);
        Subsegment sub = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", seg);
        listener.onSetEntity(seg, sub);

        Assert.assertEquals(traceID.toString() + "@" + sub.getId(), ThreadContext.get(TRACE_ID_KEY));
    }

    @Test
    public void testNestedSubsegmentInjection() {
        Log4JSegmentListener listener = (Log4JSegmentListener) AWSXRay.getGlobalRecorder().getSegmentListeners().get(0);
        listener.setPrefix("");
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceID);
        listener.onSetEntity(null, seg);
        Subsegment sub1 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test1", seg);
        listener.onSetEntity(seg, sub1);
        Subsegment sub2 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test2", seg);
        listener.onSetEntity(sub1, sub2);

        Assert.assertEquals(traceID.toString() + "@" + sub2.getId(), ThreadContext.get(TRACE_ID_KEY));
    }
}
