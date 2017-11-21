package com.amazonaws.xray;

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

import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.exceptions.AlreadyEmittedException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@FixMethodOrder(MethodSorters.JVM)
public class EntityTest {

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).build());
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

        String expected = expectedInProgressSubsegment(parent.getTraceId(), parent.getId(), subsegment.getId(), subsegment.getStartTime()).toString();

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

        String expected = expectedCompletedSegmentWithSubsegment(traceId, segment.getId(), subsegment.getId(), 1.0, subsegment.getEndTime(), segment.getEndTime()).toString();

        JSONAssert.assertEquals(expected, segment.serialize(), JSONCompareMode.NON_EXTENSIBLE);

    }

    public ObjectNode expectedCompletedSegmentWithSubsegment(TraceID traceId, String segmentId, String subsegmentId, double startTime, double subsegmentEndTime, double segmentEndTime) {
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
    @Test(expected=AlreadyEmittedException.class)
    public void testModifyingSegmentAfterEndingThrowsAlreadyEmittedException() {
        TraceID traceId = new TraceID();
        Segment segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test", traceId);
        segment.end();
        segment.setStartTime(1.0);
    }


    private static final String[] prefixes = { "set", "increment", "decrement", "put", "add" };
    private static final List<String> mutatingMethodPrefixes = Arrays.asList(prefixes);

    private static class MutatingMethodCount {
        private int mutatingMethods;
        private int mutatingMethodsThrowingExceptions;

        public MutatingMethodCount(int mutatingMethods, int mutatingMethodsThrowingExceptions) {
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
            if (mutatingMethodPrefixes.stream().anyMatch( (prefix) -> { return m.getName().startsWith(prefix); })) {
                numberOfMutatingMethods++;
                try {
                    List<Parameter> parameters = Arrays.asList(m.getParameters());
                    List<? extends Object> arguments = parameters.stream().map( (parameter) -> {
                        try {
                            Class<?> argumentClass = parameter.getType();
                            if (boolean.class.equals(argumentClass)) {
                                return false;
                            } else if (double.class.equals(argumentClass)) {
                                return 0.0d;
                            }else {
                                return argumentClass.getConstructor().newInstance();
                            }
                        } catch (NoSuchMethodException|InvocationTargetException|InstantiationException|IllegalAccessException e) {
                        }
                        return null;
                    }).collect(Collectors.toList());
                    m.invoke(entity, arguments.toArray());
                } catch (InvocationTargetException ite) {
                    if (ite.getCause() instanceof AlreadyEmittedException) {
                        numberOfMutatingMethodsThatThrewException++;
                    }
                } catch (IllegalAccessException e) {
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

    @Test(expected=AlreadyEmittedException.class)
    public void testEndingSegmentImplTwiceThrowsAlreadyEmittedException() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        segment.getName();
        segment.end();
        segment.end();
    }

    @Test(expected=AlreadyEmittedException.class)
    public void testEndingSubsegmentImplTwiceThrowsAlreadyEmittedException() {
        SegmentImpl segment = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        segment.getName();
        SubsegmentImpl subsegment = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", segment);
        segment.end();
        subsegment.end();
        subsegment.end();
    }

    @Test(expected=AlreadyEmittedException.class)
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
