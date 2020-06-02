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

import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@FixMethodOrder(MethodSorters.JVM)
public class EntityTest {

    private static final String[] PREFIXES = {"set", "increment", "decrement", "put", "add"};
    private static final List<String> MUTATING_METHOD_PREFIXES = Arrays.asList(PREFIXES);

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard()
                                                        .withEmitter(blankEmitter)
                                                        .withSamplingStrategy(defaultSamplingStrategy)
                                                        .build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testInProgressSegment() throws JSONException {
        String segmentId = Entity.generateId();
        TraceID traceId = new TraceID();

        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceId);
        segment.setId(segmentId);
        segment.setStartTime(1.0);

        String expected = expectedInProgressSegment(traceId, segmentId, segment.getStartTime()).toString();

        JSONAssert.assertEquals(expected, segment.serialize(), JSONCompareMode.NON_EXTENSIBLE);
    }


    public ObjectNode expectedInProgressSegment(TraceID traceId, String segmentId, double startTime) {
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("name", "test");
        expected.put("start_time", startTime);
        expected.put("trace_id", traceId.toString());
        expected.put("in_progress", true);
        expected.put("id", segmentId);
        return expected;
    }


    @Test
    public void testInProgressSubsegment() throws JSONException {
        Segment parent = AWSXRay.beginSegment("test");

        Subsegment subsegment = AWSXRay.beginSubsegment("test");
        subsegment.setStartTime(1.0);

        String expected = expectedInProgressSubsegment(parent.getTraceId(), parent.getId(), subsegment.getId(),
                                                       subsegment.getStartTime()).toString();

        JSONAssert.assertEquals(expected, subsegment.streamSerialize(), JSONCompareMode.NON_EXTENSIBLE);
    }

    public ObjectNode expectedInProgressSubsegment(TraceID traceId, String parentId, String subsegmentId, double startTime) {
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("name", "test");
        expected.put("start_time", startTime);
        expected.put("trace_id", traceId.toString());
        expected.put("in_progress", true);
        expected.put("id", subsegmentId);
        expected.put("type", "subsegment");
        expected.put("parent_id", parentId);
        return expected;
    }

    @Test
    public void testSegmentWithSubsegment() throws JSONException {
        TraceID traceId = new TraceID();

        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceId);
        Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        segment.addSubsegment(subsegment);

        segment.setStartTime(1.0);
        subsegment.setStartTime(1.0);

        subsegment.end();
        segment.end();

        String expected = expectedCompletedSegmentWithSubsegment(traceId, segment.getId(), subsegment.getId(), 1.0,
                                                                 subsegment.getEndTime(), segment.getEndTime()).toString();

        JSONAssert.assertEquals(expected, segment.serialize(), JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    public void testManuallySetEntityEndTime() {
        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", new TraceID());
        Subsegment subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        segment.addSubsegment(subsegment);

        double endTime = 20.0d;

        segment.setStartTime(1.0);
        subsegment.setStartTime(1.0);
        segment.setEndTime(endTime);
        subsegment.setEndTime(endTime);

        subsegment.end();
        segment.end();

        Assert.assertEquals(endTime, segment.getEndTime(), 0);
        Assert.assertEquals(endTime, subsegment.getEndTime(), 0);
    }

    public ObjectNode expectedCompletedSegmentWithSubsegment(TraceID traceId, String segmentId, String subsegmentId,
                                                             double startTime, double subsegmentEndTime, double segmentEndTime) {
        ObjectNode expected = expectedCompletedSegment(traceId, segmentId, startTime, segmentEndTime);
        expected.putArray("subsegments").add(expectedCompletedSubsegment(traceId, subsegmentId, startTime, subsegmentEndTime));
        return expected;
    }

    public ObjectNode expectedCompletedSegment(TraceID traceId, String segmentId, double startTime, double endTime) {
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("name", "test");
        expected.put("start_time", startTime);
        expected.put("end_time", endTime);
        expected.put("trace_id", traceId.toString());
        expected.put("id", segmentId);
        return expected;
    }

    public ObjectNode expectedCompletedSubsegment(TraceID traceId, String subsegmentId, double startTime, double endTime) {
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("name", "test");
        expected.put("start_time", startTime);
        expected.put("end_time", endTime);
        expected.put("id", subsegmentId);
        return expected;
    }

    @SuppressWarnings("resource")
    @Test(expected = AlreadyEmittedException.class)
    public void testModifyingSegmentAfterEndingThrowsAlreadyEmittedException() {
        TraceID traceId = new TraceID();
        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceId);
        segment.end();
        segment.setStartTime(1.0);
    }

    private static class MutatingMethodCount {
        private int mutatingMethods;
        private int mutatingMethodsThrowingExceptions;

        MutatingMethodCount(int mutatingMethods, int mutatingMethodsThrowingExceptions) {
            this.mutatingMethods = mutatingMethods;
            this.mutatingMethodsThrowingExceptions = mutatingMethodsThrowingExceptions;
        }

        public int getMutatingMethods() {
            return mutatingMethods;
        }

        public int getMutatingMethodsThrowingExceptions() {
            return mutatingMethodsThrowingExceptions;
        }
    }

    private MutatingMethodCount numberOfMutatingMethodsThatThrewException(Entity entity, Class klass) {
        int numberOfMutatingMethods = 0;
        int numberOfMutatingMethodsThatThrewException = 0;
        for (Method m : klass.getMethods()) {
            if (MUTATING_METHOD_PREFIXES.stream().anyMatch((prefix) -> {
                return m.getName().startsWith(prefix);
            })) {
                numberOfMutatingMethods++;
                try {
                    List<Parameter> parameters = Arrays.asList(m.getParameters());
                    List<?> arguments = parameters.stream().map(parameter -> {
                        try {
                            Class<?> argumentClass = parameter.getType();
                            if (boolean.class.equals(argumentClass)) {
                                return false;
                            } else if (double.class.equals(argumentClass)) {
                                return 0.0d;
                            } else {
                                return argumentClass.getConstructor().newInstance();
                            }
                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                            IllegalAccessException e) {
                            // Ignore
                        }
                        return null;
                    }).collect(Collectors.toList());
                    m.invoke(entity, arguments.toArray());
                } catch (InvocationTargetException ite) {
                    if (ite.getCause() instanceof AlreadyEmittedException) {
                        numberOfMutatingMethodsThatThrewException++;
                    }
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }
        }
        return new MutatingMethodCount(numberOfMutatingMethods, numberOfMutatingMethodsThatThrewException);
    }

    @Test
    public void testAllSegmentImplMutationMethodsThrowAlreadyEmittedExceptions() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");

        MutatingMethodCount mutationResults = numberOfMutatingMethodsThatThrewException(segment, SegmentImpl.class);
        Assert.assertEquals(0, mutationResults.getMutatingMethodsThrowingExceptions());

        segment.end();

        mutationResults = numberOfMutatingMethodsThatThrewException(segment, SegmentImpl.class);
        Assert.assertEquals(mutationResults.getMutatingMethods(), mutationResults.getMutatingMethodsThrowingExceptions());
    }

    @Test
    public void testAllSubsegmentImplMutationMethodsThrowAlreadyEmittedExceptions() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        segment.getName();
        SubsegmentImpl subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);

        MutatingMethodCount mutationResults = numberOfMutatingMethodsThatThrewException(subsegment, SubsegmentImpl.class);
        Assert.assertEquals(0, mutationResults.getMutatingMethodsThrowingExceptions());

        subsegment.setParentSegment(segment); // the mutating methods set this to null...
        segment.end();
        subsegment.end();

        mutationResults = numberOfMutatingMethodsThatThrewException(subsegment, SubsegmentImpl.class);
        Assert.assertEquals(mutationResults.getMutatingMethods(), mutationResults.getMutatingMethodsThrowingExceptions());
    }

    @Test(expected = AlreadyEmittedException.class)
    public void testEndingSegmentImplTwiceThrowsAlreadyEmittedException() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        segment.getName();
        segment.end();
        segment.end();
    }

    @Test(expected = AlreadyEmittedException.class)
    public void testEndingSubsegmentImplTwiceThrowsAlreadyEmittedException() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        segment.getName();
        SubsegmentImpl subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        segment.end();
        subsegment.end();
        subsegment.end();
    }

    @Test(expected = AlreadyEmittedException.class)
    public void testEndingSubsegmentImplAfterStreamingThrowsAlreadyEmittedException() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");

        SubsegmentImpl firstSubsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        firstSubsegment.end();

        for (int i = 0; i < 100; i++) { // add enough subsegments to trigger the DefaultStreamingStrategy and stream subsegments
            SubsegmentImpl current = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
            current.end();
        }

        segment.end();
        firstSubsegment.end();
    }
}
