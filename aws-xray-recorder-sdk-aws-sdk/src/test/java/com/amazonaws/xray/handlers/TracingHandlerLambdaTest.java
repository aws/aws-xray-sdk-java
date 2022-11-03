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

package com.amazonaws.xray.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.amazonaws.DefaultRequest;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.contexts.LambdaSegmentContext;
import com.amazonaws.xray.contexts.LambdaSegmentContextResolver;
import com.amazonaws.xray.emitters.DefaultEmitter;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.strategy.sampling.NoSamplingStrategy;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@FixMethodOrder(MethodSorters.JVM)
@PrepareForTest({LambdaSegmentContext.class, LambdaSegmentContextResolver.class})
@PowerMockIgnore("javax.net.ssl.*")
public class TracingHandlerLambdaTest {
    private static final String TRACE_HEADER =
        "Root=1-57ff426a-80c11c39b0c928905eb0828d;Parent=1234abcd1234abcd;Sampled=1";

    @Test
    public void testSamplingOverrideFalseInLambda() throws Exception {
        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Emitter mockedEmitted = Mockito.mock(DefaultEmitter.class);

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                .withEmitter(mockedEmitted)
                .build();

        lambdaTestHelper(recorder, "testFalse", false);
        Mockito.verify(mockedEmitted, Mockito.times(0)).sendSubsegment(any());
    }

    @Test
    public void testSamplingOverrideTrueInLambda() {
        Emitter mockedEmitted = Mockito.mock(DefaultEmitter.class);

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                .withSamplingStrategy(new NoSamplingStrategy())
                .withEmitter(mockedEmitted)
                .build();

        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Mockito.doAnswer(invocation -> { return true; }).when(mockedEmitted).sendSubsegment(any());

        lambdaTestHelper(recorder, "testTrue", true);
        Mockito.verify(mockedEmitted, Mockito.times(1)).sendSubsegment(any());
    }

    @Test
    public void testSamplingOverrideMixedInLambda() {
        Emitter mockedEmitted = Mockito.mock(DefaultEmitter.class);

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                .withSamplingStrategy(new NoSamplingStrategy())
                .withEmitter(mockedEmitted)
                .build();

        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(
                LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Mockito.doAnswer(invocation -> { return true; }).when(mockedEmitted).sendSubsegment(any());

        lambdaTestHelper(recorder, "test1", true);
        Mockito.verify(mockedEmitted, Mockito.times(1)).sendSubsegment(any());

        lambdaTestHelper(recorder, "test2", false);
        Mockito.verify(mockedEmitted, Mockito.times(1)).sendSubsegment(any());

        lambdaTestHelper(recorder, "test3", true);
        Mockito.verify(mockedEmitted, Mockito.times(2)).sendSubsegment(any());

        lambdaTestHelper(recorder, "test4", false);
        Mockito.verify(mockedEmitted, Mockito.times(2)).sendSubsegment(any());
    }

    public static void lambdaTestHelper(AWSXRayRecorder recorder, String name, boolean sampled) {
        if (sampled) {
            recorder.beginSubsegment(name);
        } else {
            recorder.beginSubsegmentWithoutSampling(name);
        }

        Subsegment subsegment = ((Subsegment) recorder.getTraceEntity());
        assertThat(subsegment.shouldPropagate()).isTrue();
        DefaultRequest<Void> request = new DefaultRequest<>(new InvokeRequest(), "Test");
        TracingHandler tracingHandler = new TracingHandler(recorder);
        tracingHandler.beforeRequest(request);
        TraceHeader traceHeader = TraceHeader.fromString(request.getHeaders().get(TraceHeader.HEADER_KEY));

        assertThat(traceHeader.getSampled()).isEqualTo(subsegment.isSampled() ? TraceHeader.SampleDecision.SAMPLED : TraceHeader.SampleDecision.NOT_SAMPLED);
        assertThat(traceHeader.getRootTraceId()).isEqualTo(subsegment.getTraceId());
        assertThat(traceHeader.getParentId()).isEqualTo(subsegment.getId());

        tracingHandler.afterResponse(request, new Response(new InvokeResult(), new HttpResponse(request, null)));

        recorder.endSubsegment();
    }
}
