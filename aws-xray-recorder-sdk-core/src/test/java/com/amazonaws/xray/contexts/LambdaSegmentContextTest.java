package com.amazonaws.xray.contexts;

import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.FacadeSegment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;

@FixMethodOrder(MethodSorters.JVM)
@PrepareForTest(LambdaSegmentContext.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class LambdaSegmentContextTest {

    private static final String TRACE_HEADER = "Root=1-57ff426a-80c11c39b0c928905eb0828d;Parent=1234abcd1234abcd;Sampled=1";
    private static final String TRACE_HEADER_2 = "Root=1-57ff426a-90c11c39b0c928905eb0828d;Parent=9234abcd1234abcd;Sampled=0";

    private static final String MALFORMED_TRACE_HEADER = ";;Root=1-57ff426a-80c11c39b0c928905eb0828d;;Parent=1234abcd1234abcd;;;Sampled=1;;;";

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSamplingStrategy(new LocalizedSamplingStrategy()).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testBeginSubsegmentWithNullTraceHeaderEnvironmentVariableResultsInADummySegmentParent() {
        testMockContext(TraceHeader.fromString(null), FacadeSegment.class);
    }

    @Test
    public void testBeginSubsegmentWithIncompleteTraceHeaderEnvironmentVariableResultsInADummySegmentParent() {
        testMockContext(TraceHeader.fromString("a"), FacadeSegment.class);
    }

    @Test
    public void testBeginSubsegmentWithCompleteTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testMockContext(TraceHeader.fromString(TRACE_HEADER), FacadeSegment.class);
    }

    @Test
    public void testBeginSubsegmentWithCompleteButMalformedTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testMockContext(TraceHeader.fromString(MALFORMED_TRACE_HEADER), FacadeSegment.class);
    }

    @Test
    public void testLeakedSubsegmentsAreCleanedBetweenInvocations() {

        LambdaSegmentContext lsc = new LambdaSegmentContext();

        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(TraceHeader.fromString(TRACE_HEADER));
        Subsegment firstInvocation = lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
        Assert.assertNotNull(AWSXRay.getTraceEntity());

        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(TraceHeader.fromString(TRACE_HEADER_2));
        Subsegment secondInvocation = lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
        Assert.assertNotNull(AWSXRay.getTraceEntity());

        Assert.assertTrue(FacadeSegment.class.isInstance(firstInvocation.getParent()));
        Assert.assertTrue(FacadeSegment.class.isInstance(secondInvocation.getParent()));
    }

    private void testMockContext(TraceHeader xAmznTraceId, Class<?> instanceOfClass) {
        LambdaSegmentContext mockContext = mockContext(xAmznTraceId);
        Assert.assertTrue(instanceOfClass.isInstance(mockContext.beginSubsegment(AWSXRay.getGlobalRecorder(), "test").getParent()));
        mockContext.endSubsegment(AWSXRay.getGlobalRecorder());
        Assert.assertNull(AWSXRay.getTraceEntity());
    }

    private LambdaSegmentContext mockContext(TraceHeader xAmznTraceId) {
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(xAmznTraceId);
        return new LambdaSegmentContext();
    }
}
