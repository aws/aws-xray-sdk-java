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

package com.amazonaws.xray.javax.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Cause;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.strategy.FixedSegmentNamingStrategy;
import com.amazonaws.xray.strategy.SegmentNamingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

@FixMethodOrder(MethodSorters.JVM)
public class AWSXRayServletFilterTest {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setupAWSXRay() {
        AWSXRay.setGlobalRecorder(getMockRecorder());
        AWSXRay.clearTraceEntity();
    }

    private AWSXRayRecorder getMockRecorder() {
        Emitter blankEmitter = mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        return AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSamplingStrategy(defaultSamplingStrategy).build();
    }

    private FilterChain mockChain(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<Entity> capturedEntity = new AtomicReference<>();
        Mockito.doAnswer(a -> {
            capturedEntity.set(AWSXRay.getTraceEntity());
            return null;
        }).when(chain).doFilter(request, response);

        when(request.getAttribute("com.amazonaws.xray.entities.Entity"))
                .thenAnswer(a -> Objects.requireNonNull(capturedEntity.get()));
        return chain;
    }

    @Test
    public void testAsyncServletRequestWithCompletedAsync() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenThrow(IllegalStateException.class);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = mock(AsyncEvent.class);
        when(event.getSuppliedRequest()).thenReturn(request);
        when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testAsyncServletRequestHasListenerAdded() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = mock(AsyncContext.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        verify(asyncContext, Mockito.times(1)).addListener(Mockito.any());
    }

    @Test
    public void testServletLazilyLoadsRecorder() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = mock(AsyncContext.class);
        AWSXRayRecorder customRecorder = Mockito.spy(getMockRecorder());
        AWSXRay.setGlobalRecorder(customRecorder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = mock(AsyncEvent.class);
        when(event.getSuppliedRequest()).thenReturn(request);
        when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        verify(customRecorder.getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testServletUsesPassedInRecorder() throws IOException, ServletException {
        AWSXRayRecorder customRecorder = Mockito.spy(getMockRecorder());
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter(new FixedSegmentNamingStrategy("test"), customRecorder);

        AsyncContext asyncContext = mock(AsyncContext.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = mock(AsyncEvent.class);
        when(event.getSuppliedRequest()).thenReturn(request);
        when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        verify(customRecorder.getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testAWSXRayServletAsyncListenerEmitsSegmentWhenProcessingEvent() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = mock(AsyncContext.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = mock(AsyncEvent.class);
        when(event.getSuppliedRequest()).thenReturn(request);
        when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testNameOverrideEnvironmentVariable() throws IOException, ServletException {
        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, "pass");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());

        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testSampledNoParent() throws IOException, ServletException {
        Segment segment = doSegmentTest(null, AWSXRay.getGlobalRecorder());
        assertThat(segment.isSampled()).isTrue();
        assertThat(segment.getTraceId()).isNotEqualTo(TraceID.invalid());
        assertThat(segment.getId()).isNotEmpty();
        assertThat(segment.getParentId()).isNull();
    }

    @Test
    public void testSampledWithParent() throws IOException, ServletException {
        TraceID traceID = TraceID.create();
        TraceHeader header = new TraceHeader(traceID, "1234567890123456");
        Segment segment = doSegmentTest(header.toString(), AWSXRay.getGlobalRecorder());
        assertThat(segment.isSampled()).isTrue();
        assertThat(segment.getTraceId()).isEqualTo(traceID);
        assertThat(segment.getId()).isNotEmpty();
        assertThat(segment.getParentId()).isEqualTo("1234567890123456");
    }

    private static Segment doSegmentTest(
        @Nullable String traceHeader, AWSXRayRecorder recorder) throws IOException, ServletException {

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter(SegmentNamingStrategy.fixed("test"), recorder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);
        when(request.getHeader(TraceHeader.HEADER_KEY)).thenReturn(traceHeader);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        return emittedSegment.getValue();
    }

    @Test
    public void testNameOverrideSystemProperty() throws IOException, ServletException {
        System.setProperty(SegmentNamingStrategy.NAME_OVERRIDE_SYSTEM_PROPERTY_KEY, "pass");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());
    }

    @Test
    public void testNameOverrideEnvironmentVariableOverridesSystemProperty() throws IOException, ServletException {
        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, "pass");
        System.setProperty(SegmentNamingStrategy.NAME_OVERRIDE_SYSTEM_PROPERTY_KEY, "fail");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());

        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testServletCatchesErrors() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);
        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);
        Error ourError = new StackOverflowError("Test");
        Mockito.doThrow(ourError).when(chain).doFilter(Mockito.any(), Mockito.any());

        assertThatThrownBy(() -> servletFilter.doFilter(request, response, chain)).isEqualTo(ourError);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());
        Segment segment = emittedSegment.getValue();

        Cause cause = segment.getCause();
        Assert.assertEquals(1, cause.getExceptions().size());
        Throwable storedThrowable = cause.getExceptions().get(0).getThrowable();

        Assert.assertEquals(ourError, storedThrowable);
    }

    @Test
    public void testServletCatchesExceptions() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        when(request.getMethod()).thenReturn("TEST_METHOD");
        when(request.isAsyncStarted()).thenReturn(false);
        HttpServletResponse response = mock(HttpServletResponse.class);

        FilterChain chain = mock(FilterChain.class);
        Exception ourException = new RuntimeException("Test");
        Mockito.doThrow(ourException).when(chain).doFilter(Mockito.any(), Mockito.any());


        assertThatThrownBy(() -> servletFilter.doFilter(request, response, chain)).isEqualTo(ourException);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());
        Segment segment = emittedSegment.getValue();

        Cause cause = segment.getCause();
        Assert.assertEquals(1, cause.getExceptions().size());
        Throwable storedThrowable = cause.getExceptions().get(0).getThrowable();

        Assert.assertEquals(ourException, storedThrowable);
    }
}
