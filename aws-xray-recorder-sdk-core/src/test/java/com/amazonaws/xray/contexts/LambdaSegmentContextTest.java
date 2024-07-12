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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.FacadeSegment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.exceptions.SubsegmentNotFoundException;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.RuntimeErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LambdaSegmentContextTest {

    private static final String TRACE_HEADER = "Root=1-57ff426a-80c11c39b0c928905eb0828d;Parent=1234abcd1234abcd;Sampled=1";
    private static final String TRACE_HEADER_2 = "Root=1-57ff426a-90c11c39b0c928905eb0828d;Parent=9234abcd1234abcd;Sampled=0";

    private static final String MALFORMED_TRACE_HEADER =
        ";;Root=1-57ff426a-80c11c39b0c928905eb0828d;;Parent=1234abcd1234abcd;;;Sampled=1;;;";

    @BeforeEach
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard()
                                                        .withEmitter(blankEmitter)
                                                        .withSamplingStrategy(new LocalizedSamplingStrategy())
                                                        .build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testBeginSubsegmentWithNullTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testContextResultsInFacadeSegmentParent();
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = "a")
    void testBeginSubsegmentWithIncompleteTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testContextResultsInFacadeSegmentParent();
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = TRACE_HEADER)
    void testBeginSubsegmentWithCompleteTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testContextResultsInFacadeSegmentParent();
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = MALFORMED_TRACE_HEADER)
    void testBeginSubsegmentWithCompleteButMalformedTraceHeaderEnvironmentVariableResultsInAFacadeSegmentParent() {
        testContextResultsInFacadeSegmentParent();
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = TRACE_HEADER_2)
    void testNotSampledSetsParentToSubsegment() {
        LambdaSegmentContext lsc = new LambdaSegmentContext();
        lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
        lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test2");
        lsc.endSubsegment(AWSXRay.getGlobalRecorder());
        lsc.endSubsegment(AWSXRay.getGlobalRecorder());
    }

    @Test
    @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = TRACE_HEADER)
    void testEndSubsegmentUsesContextMissing() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new LogErrorContextMissingStrategy());
        AWSXRay.endSubsegment(); // No exception

        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new RuntimeErrorContextMissingStrategy());
        assertThatThrownBy(AWSXRay::endSubsegment).isInstanceOf(SubsegmentNotFoundException.class);
    }

    // We create segments twice with different environment variables for the same context, similar to how Lambda would invoke
    // a function.
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    static class LeakedSubsegments {
        LambdaSegmentContext lsc;

        @BeforeAll
        void setupContext() {
            lsc = new LambdaSegmentContext();
        }

        @Test
        @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = TRACE_HEADER)
        void oneInvocationGeneratesSegment() {
            Subsegment firstInvocation = lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
            assertThat(firstInvocation).isNotNull();
            assertThat(firstInvocation.getParent()).isInstanceOf(FacadeSegment.class);
        }

        @Test
        @SetEnvironmentVariable(key = "_X_AMZN_TRACE_ID", value = TRACE_HEADER_2)
        void anotherInvocationGeneratesSegment() {
            Subsegment secondInvocation = lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
            assertThat(secondInvocation).isNotNull();
            assertThat(secondInvocation.getParent()).isInstanceOf(FacadeSegment.class);
        }

        @Test
        @SetSystemProperty(key = "com.amazonaws.xray.traceHeader", value = TRACE_HEADER)
        void oneInvocationGeneratesSegmentUsingSystemProperty() {
            Subsegment firstInvocation = lsc.beginSubsegment(AWSXRay.getGlobalRecorder(), "test");
            assertThat(firstInvocation).isNotNull();
            assertThat(firstInvocation.getParent()).isInstanceOf(FacadeSegment.class);
        }
    }

    private static void testContextResultsInFacadeSegmentParent() {
        LambdaSegmentContext mockContext = new LambdaSegmentContext();
        assertThat(mockContext.beginSubsegment(AWSXRay.getGlobalRecorder(), "test").getParent())
            .isInstanceOf(FacadeSegment.class);
        mockContext.endSubsegment(AWSXRay.getGlobalRecorder());
        assertThat(AWSXRay.getTraceEntity()).isNull();
    }
}
