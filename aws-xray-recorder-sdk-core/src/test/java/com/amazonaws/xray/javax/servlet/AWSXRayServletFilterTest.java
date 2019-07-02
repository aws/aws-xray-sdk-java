package com.amazonaws.xray.javax.servlet;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.strategy.FixedSegmentNamingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
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

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.strategy.SegmentNamingStrategy;

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
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        return AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSamplingStrategy(defaultSamplingStrategy).build();
    }

    private FilterChain mockChain(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        FilterChain chain = Mockito.mock(FilterChain.class);
        AtomicReference<Entity> capturedEntity = new AtomicReference<>();
        Mockito.doAnswer(a -> {
            capturedEntity.set(AWSXRay.getTraceEntity());
            return null;
        }).when(chain).doFilter(request, response);

        Mockito.when(request.getAttribute("com.amazonaws.xray.entities.Entity"))
                .thenAnswer(a -> Objects.requireNonNull(capturedEntity.get()));
        return chain;
    }
    @Test
    public void testAsyncServletRequestWithCompletedAsync() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenThrow(IllegalStateException.class);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = Mockito.mock(AsyncEvent.class);
        Mockito.when(event.getSuppliedRequest()).thenReturn(request);
        Mockito.when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        Mockito.verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }
    @Test
    public void testAsyncServletRequestHasListenerAdded() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = Mockito.mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        Mockito.verify(asyncContext, Mockito.times(1)).addListener(Mockito.any());
    }

    @Test
    public void testServletLazilyLoadsRecorder() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
        AWSXRayRecorder customRecorder = getMockRecorder();
        Mockito.spy(customRecorder);
        AWSXRay.setGlobalRecorder(customRecorder);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = Mockito.mock(AsyncEvent.class);
        Mockito.when(event.getSuppliedRequest()).thenReturn(request);
        Mockito.when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        Mockito.verify(customRecorder.getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testServletUsesPassedInRecorder() throws IOException, ServletException {
        AWSXRayRecorder customRecorder = getMockRecorder();
        Mockito.spy(customRecorder);
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter(new FixedSegmentNamingStrategy("test"), customRecorder);

        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = Mockito.mock(AsyncEvent.class);
        Mockito.when(event.getSuppliedRequest()).thenReturn(request);
        Mockito.when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        Mockito.verify(customRecorder.getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testAWSXRayServletAsyncListenerEmitsSegmentWhenProcessingEvent() throws IOException, ServletException {
        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("test");

        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(true);
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = mockChain(request, response);

        AsyncEvent event = Mockito.mock(AsyncEvent.class);
        Mockito.when(event.getSuppliedRequest()).thenReturn(request);
        Mockito.when(event.getSuppliedResponse()).thenReturn(response);

        servletFilter.doFilter(request, response, chain);
        Assert.assertNull(AWSXRay.getTraceEntity());

        AWSXRayServletAsyncListener listener = (AWSXRayServletAsyncListener) Whitebox.getInternalState(servletFilter, "listener");
        listener.onComplete(event);

        Mockito.verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testNameOverrideEnvironmentVariable() throws IOException, ServletException {
        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, "pass");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = Mockito.mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        Mockito.verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());

        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testNameOverrideSystemProperty() throws IOException, ServletException {
        System.setProperty(SegmentNamingStrategy.NAME_OVERRIDE_SYSTEM_PROPERTY_KEY, "pass");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = Mockito.mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        Mockito.verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());
    }

    @Test
    public void testNameOverrideEnvironmentVariableOverridesSystemProperty() throws IOException, ServletException {
        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, "pass");
        System.setProperty(SegmentNamingStrategy.NAME_OVERRIDE_SYSTEM_PROPERTY_KEY, "fail");

        AWSXRayServletFilter servletFilter = new AWSXRayServletFilter("fail");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("test_url"));
        Mockito.when(request.getMethod()).thenReturn("TEST_METHOD");
        Mockito.when(request.isAsyncStarted()).thenReturn(false);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        FilterChain chain = Mockito.mock(FilterChain.class);

        servletFilter.doFilter(request, response, chain);

        ArgumentCaptor<Segment> emittedSegment = ArgumentCaptor.forClass(Segment.class);
        Mockito.verify(AWSXRay.getGlobalRecorder().getEmitter(), Mockito.times(1)).sendSegment(emittedSegment.capture());

        Assert.assertEquals("pass", emittedSegment.getValue().getName());

        environmentVariables.set(SegmentNamingStrategy.NAME_OVERRIDE_ENVIRONMENT_VARIABLE_KEY, null);
    }
}
