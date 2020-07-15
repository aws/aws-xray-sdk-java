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

package com.amazonaws.xray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.xray.contexts.LambdaSegmentContext;
import com.amazonaws.xray.contexts.LambdaSegmentContextResolver;
import com.amazonaws.xray.contexts.SegmentContextResolverChain;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.amazonaws.xray.exceptions.SegmentNotFoundException;
import com.amazonaws.xray.plugins.EC2Plugin;
import com.amazonaws.xray.plugins.ECSPlugin;
import com.amazonaws.xray.plugins.EKSPlugin;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import com.amazonaws.xray.plugins.Plugin;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.LogErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.RuntimeErrorContextMissingStrategy;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@FixMethodOrder(MethodSorters.JVM)
@PrepareForTest({LambdaSegmentContext.class, LambdaSegmentContextResolver.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class AWSXRayRecorderTest {

    private static final String TRACE_HEADER = "Root=1-57ff426a-80c11c39b0c928905eb0828d;Parent=1234abcd1234abcd;Sampled=1";

    private static ExecutorService threadExecutor;

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @BeforeClass
    public static void startExecutor() {
        threadExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void stopExecutor() {
        threadExecutor.shutdown();
    }

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder
                                      .standard()
                                      .withEmitter(blankEmitter)
                                      .withSamplingStrategy(defaultSamplingStrategy)
                                      .build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testGetThreadLocalReturnsCurrentSegment() {
        Segment segment = AWSXRay.beginSegment("test");
        Assert.assertEquals(segment, AWSXRay.getTraceEntity());
        AWSXRay.endSegment();
    }

    @Test
    public void testGetTraceEntityReturnsCurrentSegment() {
        Segment segment = AWSXRay.beginSegment("test");
        Assert.assertEquals(segment, AWSXRay.getTraceEntity());
        AWSXRay.endSegment();
    }

    @Test
    public void testGetThreadLocalReturnsCurrentSubsegment() {
        AWSXRay.beginSegment("test");
        Subsegment subsegment = AWSXRay.beginSubsegment("test");
        Assert.assertEquals(subsegment, AWSXRay.getTraceEntity());
        AWSXRay.endSubsegment();
        AWSXRay.endSegment();
    }

    @Test
    public void testGetTraceEntityReturnsCurrentSubsegment() {
        AWSXRay.beginSegment("test");
        Subsegment subsegment = AWSXRay.beginSubsegment("test");
        Assert.assertEquals(subsegment, AWSXRay.getTraceEntity());
        AWSXRay.endSubsegment();
        AWSXRay.endSegment();
    }

    @Test
    public void testGetThreadLocalOnEmptyThreadDoesNotThrowException() {
        AWSXRay.beginSegment("test");
        AWSXRay.endSegment();
        AWSXRay.getTraceEntity();
    }

    @Test
    public void testGetTraceEntityOnEmptyThreadDoesNotThrowException() {
        AWSXRay.beginSegment("test");
        AWSXRay.endSegment();
        AWSXRay.getTraceEntity();
    }

    @Test(expected = SegmentNotFoundException.class)
    public void testBeginSubsegmentOnEmptyThreadThrowsExceptionByDefault() {
        AWSXRay.beginSubsegment("test");
    }

    @Test
    public void testBeginSubsegmentOnEmptyThreadDoesNotThrowExceptionWithLogErrorContextMissingStrategy() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new LogErrorContextMissingStrategy());
        AWSXRay.beginSubsegment("test");
    }

    @Test
    public void testBeginSubsegmentOnEmptyThreadDoesNotThrowExceptionWithIgnoreErrorContextMissingStrategy() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        AWSXRay.beginSubsegment("test");
    }

    @Test
    public void testInjectThreadLocalInjectsCurrentSegment() throws Exception {
        Segment segment = AWSXRay.beginSegment("test");

        threadExecutor.submit(() -> {
            AWSXRay.injectThreadLocal(segment);
            Assert.assertEquals(segment, AWSXRay.getThreadLocal());
        }).get();

        AWSXRay.endSegment();
    }

    @Test
    public void testSetTraceEntityInjectsCurrentSegment() throws Exception {
        Segment segment = AWSXRay.beginSegment("test");

        threadExecutor.submit(() -> {
            AWSXRay.setTraceEntity(segment);
            Assert.assertEquals(segment, AWSXRay.getTraceEntity());
        }).get();

        AWSXRay.endSegment();
    }

    @Test
    public void testInjectThreadLocalInjectsCurrentSubsegment() throws Exception {
        AWSXRay.beginSegment("test");
        Subsegment subsegment = AWSXRay.beginSubsegment("test");

        threadExecutor.submit(() -> {
            AWSXRay.injectThreadLocal(subsegment);
            Assert.assertEquals(subsegment, AWSXRay.getThreadLocal());
        }).get();

        AWSXRay.endSubsegment();
        AWSXRay.endSegment();
    }

    @Test
    public void testSetTraceEntityInjectsCurrentSubsegment() throws Exception {
        AWSXRay.beginSegment("test");
        Subsegment subsegment = AWSXRay.beginSubsegment("test");

        threadExecutor.submit(() -> {
            AWSXRay.setTraceEntity(subsegment);
            Assert.assertEquals(subsegment, AWSXRay.getThreadLocal());
        }).get();

        AWSXRay.endSubsegment();
        AWSXRay.endSegment();
    }

    @Test
    public void testIsCurrentSegmentPresent() {
        Assert.assertFalse(AWSXRay.getCurrentSegmentOptional().isPresent());
        AWSXRay.beginSegment("test");
        Assert.assertTrue(AWSXRay.getCurrentSegmentOptional().isPresent());
        AWSXRay.endSegment();
        Assert.assertFalse(AWSXRay.getCurrentSegmentOptional().isPresent());
    }

    @Test
    public void testIsCurrentSubsegmentPresent() {
        Assert.assertFalse(AWSXRay.getCurrentSubsegmentOptional().isPresent());

        AWSXRay.beginSegment("test");
        Assert.assertFalse(AWSXRay.getCurrentSubsegmentOptional().isPresent());

        AWSXRay.beginSubsegment("test");
        Assert.assertTrue(AWSXRay.getCurrentSubsegmentOptional().isPresent());

        AWSXRay.endSubsegment();
        Assert.assertFalse(AWSXRay.getCurrentSubsegmentOptional().isPresent());

        AWSXRay.endSegment();
        Assert.assertFalse(AWSXRay.getCurrentSubsegmentOptional().isPresent());
    }

    @Test(expected = SegmentNotFoundException.class)
    public void testSubsegmentBeginWithoutSegmentContextThrowsException() {
        AWSXRay.beginSubsegment("test");
    }

    @Test
    public void testNotSendingUnsampledSegment() {
        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        Segment segment = recorder.beginSegment("test");
        segment.setSampled(false);
        recorder.endSegment();

        Mockito.verify(mockEmitter, Mockito.times(0)).sendSegment(Mockito.any());
    }

    @Test
    public void testSegmentEmitted() {
        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        recorder.beginSegment("test");
        recorder.beginSubsegment("test");
        recorder.endSubsegment();
        recorder.endSegment();

        Mockito.verify(mockEmitter, Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testExplicitSubsegmentEmitted() {
        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        recorder.beginSegment("test");
        Subsegment subsegment = recorder.beginSubsegment("test");
        recorder.endSubsegment(subsegment);
        recorder.endSegment();

        Mockito.verify(mockEmitter, Mockito.times(1)).sendSegment(Mockito.any());
    }

    @Test
    public void testDummySegmentNotEmitted() {
        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        recorder.beginDummySegment();
        recorder.beginSubsegment("test");
        recorder.endSubsegment();
        recorder.endSegment();

        Mockito.verify(mockEmitter, Mockito.times(0)).sendSegment(Mockito.any());
    }

    @Test
    public void testSubsegmentEmittedInLambdaContext() throws JSONException {
        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();
        recorder.createSubsegment("test", () -> {
        });

        ArgumentCaptor<Subsegment> emittedSubsegment = ArgumentCaptor.forClass(Subsegment.class);
        Mockito.verify(mockEmitter, Mockito.times(1)).sendSubsegment(emittedSubsegment.capture());

        Subsegment captured = emittedSubsegment.getValue();

        JSONAssert.assertEquals(expectedLambdaSubsegment(
            header.getRootTraceId(), header.getParentId(), captured.getId(), captured.getStartTime(),
            captured.getEndTime()).toString(), captured.streamSerialize(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private ObjectNode expectedLambdaSubsegment(
        TraceID traceId, String segmentId, String subsegmentId, double startTime, double endTime) {
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("name", "test");
        expected.put("type", "subsegment");
        expected.put("start_time", startTime);
        expected.put("end_time", endTime);
        expected.put("trace_id", traceId.toString());
        expected.put("parent_id", segmentId);
        expected.put("id", subsegmentId);
        return expected;
    }

    @Test
    public void testSubsegmentNotEmittedWithoutExceptionInLambdaInitContext() {
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment"))
                    .toReturn(TraceHeader.fromString(null));
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();
        recorder.createSubsegment("test", () -> {
        });

        Mockito.verify(mockEmitter, Mockito.times(0)).sendSubsegment(Mockito.any());
    }

    @Test
    public void testSubsegmentWithChildEmittedTogetherInLambdaContext() {
        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        recorder.createSubsegment("testTogether", () -> {
            recorder.createSubsegment("testTogether2", () -> {
            });
        });

        ArgumentCaptor<Subsegment> emittedSubsegment = ArgumentCaptor.forClass(Subsegment.class);
        Mockito.verify(mockEmitter, Mockito.times(1)).sendSubsegment(emittedSubsegment.capture());

        Subsegment captured = emittedSubsegment.getValue();

        Assert.assertEquals(1, captured.getSubsegments().size());
    }

    @Test
    public void testSubsequentSubsegmentBranchesEmittedInLambdaContext() {
        TraceHeader header = TraceHeader.fromString(TRACE_HEADER);

        PowerMockito.stub(PowerMockito.method(LambdaSegmentContext.class, "getTraceHeaderFromEnvironment")).toReturn(header);
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn("/var/task");

        Emitter mockEmitter = Mockito.mock(Emitter.class);
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withEmitter(mockEmitter).build();

        recorder.createSubsegment("testTogether", () -> {
            recorder.createSubsegment("testTogether2", () -> {
            });
        });

        recorder.createSubsegment("testTogether3", () -> {
            recorder.createSubsegment("testTogether4", () -> {
            });
        });

        ArgumentCaptor<Subsegment> emittedSubsegments = ArgumentCaptor.forClass(Subsegment.class);

        Mockito.verify(mockEmitter, Mockito.times(2)).sendSubsegment(emittedSubsegments.capture());

        List<Subsegment> captured = emittedSubsegments.getAllValues();

        captured.forEach((capturedSubsegment) -> {
            Assert.assertEquals(1, capturedSubsegment.getSubsegments().size());
        });
    }

    @Test
    public void testContextMissingStrategyOverrideEnvironmentVariable() {
        environmentVariables.set(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY, "log_error");
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withContextMissingStrategy(
            new RuntimeErrorContextMissingStrategy()).build();
        Assert.assertTrue(recorder.getContextMissingStrategy() instanceof LogErrorContextMissingStrategy);
        environmentVariables.set(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY, null);
    }

    @Test
    public void testContextMissingStrategyOverrideSystemProperty() {
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "log_error");
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withContextMissingStrategy(
            new RuntimeErrorContextMissingStrategy()).build();
        Assert.assertTrue(recorder.getContextMissingStrategy() instanceof LogErrorContextMissingStrategy);
    }

    @Test
    public void testContextMissingStrategyOverrideEnvironmentVariableOverridesSystemProperty() {
        environmentVariables.set(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY, "log_error");
        System.setProperty(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_SYSTEM_PROPERTY_OVERRIDE_KEY, "runtime_error");
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard().withContextMissingStrategy(
            new RuntimeErrorContextMissingStrategy()).build();
        Assert.assertTrue(recorder.getContextMissingStrategy() instanceof LogErrorContextMissingStrategy);
        environmentVariables.set(ContextMissingStrategy.CONTEXT_MISSING_STRATEGY_ENVIRONMENT_VARIABLE_OVERRIDE_KEY, null);
    }

    @Test(expected = AlreadyEmittedException.class)
    public void testEmittingSegmentTwiceThrowsSegmentAlreadyEmittedException() {
        Segment s = AWSXRay.beginSegment("test");
        AWSXRay.endSegment();
        AWSXRay.injectThreadLocal(s);
        AWSXRay.endSegment();
    }

    @Test
    public void testBeginSegmentWhenMissingContext() {
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withSegmentContextResolverChain(new SegmentContextResolverChain())
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();
        Segment segment = recorder.beginSegment("hello");
        assertThat(segment).isNotNull();
        assertThat(segment.getNamespace()).isEmpty();
        // No-op
        segment.setNamespace("foo");
        assertThat(segment.getNamespace()).isEmpty();
    }

    @Test
    public void testBeginSubsegmentWhenMissingContext() {
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();
        Subsegment subsegment = recorder.beginSubsegment("hello");
        assertThat(subsegment).isNotNull();
        assertThat(subsegment.getNamespace()).isEmpty();
        // No-op
        subsegment.setNamespace("foo");
        assertThat(subsegment.getNamespace()).isEmpty();
    }

    @Test
    public void testSubsegmentFunctionExceptionWhenMissingContext() {
        IllegalStateException expectedException = new IllegalStateException("To be thrown by function");
        Function<Subsegment, Void> function = (subsegment) -> {
            throw expectedException;
        };

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();

        assertThatThrownBy(() -> recorder.createSubsegment("test", function)).isEqualTo(expectedException);
    }

    @Test
    public void testSubsegmentConsumerExceptionWhenMissingContext() {
        IllegalStateException expectedException = new IllegalStateException("To be thrown by consumer");
        Consumer<Subsegment> consumer = (subsegment) -> {
            throw expectedException;
        };
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();

        assertThatThrownBy(() -> recorder.createSubsegment("test", consumer)).isEqualTo(expectedException);
    }

    @Test
    public void testSubsegmentSupplierExceptionWhenMissingContext() {
        RuntimeException expectedException = new RuntimeException("To be thrown by supplier");
        Supplier<Void> supplier = () -> {
            throw expectedException;
        };
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();

        assertThatThrownBy(() -> recorder.createSubsegment("test", supplier)).isEqualTo(expectedException);
    }

    @Test
    public void testSubsegmentRunnableExceptionWhenMissingContext() {
        RuntimeException expectedException = new RuntimeException("To be thrown by runnable");
        Runnable runnable = () -> {
            throw expectedException;
        };
        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withContextMissingStrategy(new IgnoreErrorContextMissingStrategy())
                                                         .build();

        assertThatThrownBy(() -> recorder.createSubsegment("test", runnable)).isEqualTo(expectedException);
    }

    @Test
    public void testOriginResolutionWithAllPlugins() {
        //given
        EC2Plugin ec2Plugin = Mockito.mock(EC2Plugin.class);
        ECSPlugin ecsPlugin = Mockito.mock(ECSPlugin.class);
        ElasticBeanstalkPlugin ebPlugin = Mockito.mock(ElasticBeanstalkPlugin.class);
        EKSPlugin eksPlugin = Mockito.mock(EKSPlugin.class);

        List<Plugin> plugins = new ArrayList<>();
        plugins.add(ec2Plugin);
        plugins.add(ecsPlugin);
        plugins.add(ebPlugin);
        plugins.add(eksPlugin);

        List<String> origins = new ArrayList<>();
        origins.add(EC2Plugin.ORIGIN);
        origins.add(ECSPlugin.ORIGIN);
        origins.add(ElasticBeanstalkPlugin.ORIGIN);
        origins.add(EKSPlugin.ORIGIN);

        Map<String, Object> runtimeContext = new HashMap<>();
        runtimeContext.put("key", "value");

        for (int i = 0; i < 4; i++) {
            Mockito.doReturn(true).when(plugins.get(i)).isEnabled();
            Mockito.doReturn(runtimeContext).when(plugins.get(i)).getRuntimeContext();
            Mockito.doReturn("serviceName").when(plugins.get(i)).getServiceName();
            Mockito.doReturn(origins.get(i)).when(plugins.get(i)).getOrigin();
        }

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withPlugin(ec2Plugin)
                                                         .withPlugin(ecsPlugin)
                                                         .withPlugin(ebPlugin)
                                                         .withPlugin(eksPlugin)
                                                         .build();

        // when
        Assert.assertEquals(ElasticBeanstalkPlugin.ORIGIN, recorder.getOrigin());
    }

    @Test
    public void testEKSOriginResolvesOverECSOrigin() {
        //given
        ECSPlugin ecsPlugin = Mockito.mock(ECSPlugin.class);
        EKSPlugin eksPlugin = Mockito.mock(EKSPlugin.class);

        Map<String, Object> runtimeContext = new HashMap<>();
        runtimeContext.put("key", "value");

        Mockito.doReturn(true).when(ecsPlugin).isEnabled();
        Mockito.doReturn(runtimeContext).when(ecsPlugin).getRuntimeContext();
        Mockito.doReturn("ecs").when(ecsPlugin).getServiceName();
        Mockito.doReturn(ECSPlugin.ORIGIN).when(ecsPlugin).getOrigin();
        Mockito.doReturn(true).when(eksPlugin).isEnabled();
        Mockito.doReturn(runtimeContext).when(eksPlugin).getRuntimeContext();
        Mockito.doReturn("eks").when(eksPlugin).getServiceName();
        Mockito.doReturn(EKSPlugin.ORIGIN).when(eksPlugin).getOrigin();

        AWSXRayRecorder recorder = AWSXRayRecorderBuilder.standard()
                                                         .withPlugin(eksPlugin)
                                                         .withPlugin(ecsPlugin)
                                                         .build();

        // when
        Assert.assertEquals(EKSPlugin.ORIGIN, recorder.getOrigin());
    }

    @Test
    public void testPluginEquality() {
        Collection<Plugin> plugins = new HashSet<>();

        plugins.add(new EC2Plugin());
        plugins.add(new EC2Plugin()); // should be deduped
        plugins.add(new ECSPlugin());
        plugins.add(new EKSPlugin());
        plugins.add(new ElasticBeanstalkPlugin());

        Assert.assertEquals(4, plugins.size());
    }

    @Test
    public void testCurrentFormattedId() {
        TraceID traceId = TraceID.fromString("1-5759e988-bd862e3fe1be46a994272793");
        String entityId = "123456789";

        Segment seg = AWSXRay.beginSegment("test", traceId, "FakeParentId");
        seg.setId(entityId);

        Assert.assertEquals(traceId.toString() + "@" + entityId, AWSXRay.currentFormattedId());
    }
}
