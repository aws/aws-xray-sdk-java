package com.amazonaws.xray.strategy;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.*;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@FixMethodOrder(MethodSorters.JVM)
public class DefaultStreamingStrategyTest {

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSamplingStrategy(defaultSamplingStrategy).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testDefaultStreamingStrategyRequiresStreaming() {
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        Segment smallSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "small");
        Assert.assertFalse(defaultStreamingStrategy.requiresStreaming(smallSegment));

        Segment bigSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "big");
        bigSegment.addSubsegment(new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "big_child", bigSegment));
        bigSegment.addSubsegment(new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "big_child", bigSegment));
        Assert.assertTrue(defaultStreamingStrategy.requiresStreaming(bigSegment));
    }

    @Test
    public void testDefaultStreamingStrategyDoesNotRequireStreaming() {
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);
        Segment smallSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "small");
        Assert.assertFalse(defaultStreamingStrategy.requiresStreaming(smallSegment));
    }

    @Test
    public void testingBasicStreamingFunctionality() {
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);
        TraceID traceId = new TraceID();

        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceId);
        Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        Subsegment subsegment1 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        segment.addSubsegment(subsegment);
        segment.addSubsegment(subsegment1);

        segment.setStartTime(1.0);
        subsegment.setStartTime(1.0);
        subsegment1.setStartTime(1.0);

        subsegment.end();

        defaultStreamingStrategy.streamSome(segment, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(segment.getTotalSize().intValue() == 1);
    }
    @Test
    public void testStreamSomeChildrenRemovedFromParent() { //test to see if the correct actions are being taken in streamSome (children get removed from parent)
        TraceID traceId = new TraceID();
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        Segment bigSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "big", traceId);
        bigSegment.setStartTime(1.0);

        for (int i = 0; i < 5; i++) {
            Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "child"+i, bigSegment);
            subsegment.setStartTime(1.0);
            bigSegment.addSubsegment(subsegment);
            subsegment.end();
        }
        Assert.assertTrue(defaultStreamingStrategy.requiresStreaming(bigSegment));
        defaultStreamingStrategy.streamSome(bigSegment, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(bigSegment.getTotalSize().intValue() == 0);
    }

    @Test
    public void testStreamSomeChildrenNotRemovedFromParent() { //test to see if the correct actions are being taken in streamSome (children do NOT get removed from parent due to subsegments being in progress.)
        TraceID traceId = new TraceID();
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        Segment bigSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "big", traceId);
        bigSegment.setStartTime(1.0);

        for (int i = 0; i < 5; i++) {
            Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "child"+i, bigSegment);
            subsegment.setStartTime(1.0);
            bigSegment.addSubsegment(subsegment);
        }
        Assert.assertTrue(defaultStreamingStrategy.requiresStreaming(bigSegment));
        defaultStreamingStrategy.streamSome(bigSegment, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(bigSegment.getTotalSize().intValue() == 5);
    }

    @Test
    public void testMultithreadedStreamSome() {
        TraceID traceId = new TraceID();
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        Segment segment = AWSXRay.beginSegment("big");


        Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "subsegment1", segment);
        subsegment.setStartTime(1.0);
        segment.addSubsegment(subsegment);
        subsegment.end();

        Thread thread1 = new Thread(() -> {
            AWSXRay.setTraceEntity(segment);
            AWSXRay.beginSubsegment("thread1");
            AWSXRay.endSubsegment();
        });
        Thread thread2 = new Thread(() -> {
            AWSXRay.setTraceEntity(segment);
            AWSXRay.beginSubsegment("thread2");
            AWSXRay.endSubsegment();
        });

        thread1.start();
        thread2.start();
        for (Thread thread : new Thread[]{thread1, thread2}) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                return;
            }
        }

        Assert.assertTrue(AWSXRay.getTraceEntity().getName().equals("big"));
        Assert.assertTrue(AWSXRay.getTraceEntity().getTotalSize().intValue() == 3); //asserts that all subsegments are added correctly.

        defaultStreamingStrategy.streamSome(segment, AWSXRay.getGlobalRecorder().getEmitter());

        Assert.assertTrue(segment.getTotalSize().intValue() == 0);
    }

    @Test
    public void testBushyandSpindlySegmentTreeStreaming() {
        TraceID traceId = new TraceID();
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        Segment bigSegment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "big", traceId);
        bigSegment.setStartTime(1.0);

        for (int i = 0; i < 5; i++) {
            Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "child"+i, bigSegment);
            subsegment.setStartTime(1.0);
            bigSegment.addSubsegment(subsegment);
            subsegment.end();
        }

        SubsegmentImpl holder = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "big_child0", bigSegment);
        holder.setStartTime(1.0);
        bigSegment.addSubsegment(holder);
        holder.end();

        SubsegmentImpl holder1 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "big_child1", bigSegment);
        holder1.setStartTime(1.0);
        bigSegment.addSubsegment(holder1);
        holder1.end();

        SubsegmentImpl holder2 = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "big_child2", bigSegment);
        holder2.setStartTime(1.0);
        bigSegment.addSubsegment(holder2);
        holder2.end();

        Assert.assertTrue(defaultStreamingStrategy.requiresStreaming(bigSegment));
        defaultStreamingStrategy.streamSome(bigSegment, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(bigSegment.getReferenceCount() == 0);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultStreamingStrategyMaxSegmentSizeParameterValidation() {
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(-1);
    }

    @Test
    public void testDefaultStreamingStrategyForLambdaTraceContext() { //test to see if FacadeSegment can be streamed out correctly
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(1);

        //if FacadeSegment size is larger than maxSegmentSize and only the first subsegment is completed, first subsegment will be streamed out
        FacadeSegment facadeSegmentOne = new FacadeSegment(AWSXRay.getGlobalRecorder(), new TraceID(), "", TraceHeader.SampleDecision.SAMPLED);
        Subsegment firstSubsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "FirstSubsegment", facadeSegmentOne);
        Subsegment secondSubsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "SecondSubsegment", facadeSegmentOne);
        facadeSegmentOne.addSubsegment(firstSubsegment);
        facadeSegmentOne.addSubsegment(secondSubsegment);

        firstSubsegment.end();

        Assert.assertTrue(facadeSegmentOne.getTotalSize().intValue() == 2);
        defaultStreamingStrategy.streamSome(facadeSegmentOne, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(facadeSegmentOne.getTotalSize().intValue() == 1);

        Subsegment tempOne = facadeSegmentOne.getSubsegments().get(0);
        Assert.assertEquals("SecondSubsegment", tempOne.getName());

        //if FarcadeSegment size is larger than maxSegmentSize and only the second subsegment is completed, second subsegment will be streamed out
        FacadeSegment facadeSegmentTwo = new FacadeSegment(AWSXRay.getGlobalRecorder(), new TraceID(), "", TraceHeader.SampleDecision.SAMPLED);
        Subsegment thirdSubsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "ThirdSubsegment", facadeSegmentTwo);
        Subsegment fourthSubsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "FourthSubsegment", facadeSegmentTwo);
        facadeSegmentTwo.addSubsegment(thirdSubsegment);
        facadeSegmentTwo.addSubsegment(fourthSubsegment);

        fourthSubsegment.end();

        Assert.assertTrue(facadeSegmentTwo.getTotalSize().intValue() == 2);
        defaultStreamingStrategy.streamSome(facadeSegmentTwo, AWSXRay.getGlobalRecorder().getEmitter());
        Assert.assertTrue(facadeSegmentTwo.getTotalSize().intValue() == 1);

        Subsegment tempTwo = facadeSegmentTwo.getSubsegments().get(0);
        Assert.assertEquals("ThirdSubsegment", tempTwo.getName());
    }
}