package com.amazonaws.xray.strategy;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.SubsegmentImpl;

@FixMethodOrder(MethodSorters.JVM)
public class DefaultStreamingStrategyTest {

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

    @SuppressWarnings("unused")
    @Test(expected=IllegalArgumentException.class)
    public void testDefaultStreamingStrategyMaxSegmentSizeParameterValidation() {
        DefaultStreamingStrategy defaultStreamingStrategy = new DefaultStreamingStrategy(-1);
    }
}
