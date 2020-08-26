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

package com.amazonaws.xray.entities;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@Fork(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class EntitySerializerBenchmark {
    public static final String SEGMENT_NAME = "BENCHMARK_SEGMENT";
    public static final String SUBSEGMENT_NAME = "BENCHMARK_SUBSEGMENT";


    // Populate the entity with common metadata to make the conditions for serialization
    // more practical.
    private static void populateEntity(Entity entity) {
        entity.putAnnotation("Annotation", "Data");
        entity.putMetadata("Metadata", "Value");

        Map<String, Object> request = new HashMap<>();
        request.put("url", "http://localhost:8000/");
        request.put("method", "POST");
        request.put("user_agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36");
        request.put("client_ip", "127.0.0.1");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "200");

        Map<String, Object> aws = new HashMap<>();
        aws.put("sdk_version", "2.2.x-INTERNAL");
        aws.put("sdk", "X-Ray for Java");

        entity.putAllHttp(request);
        entity.putAllHttp(response);
        entity.putAllAws(aws);
    }

    // Remove all the child subsegments from the given segment.
    private static void clearSegment(Segment parentSegment) {
        List<Subsegment> toRemove = new ArrayList<>(parentSegment.getSubsegments());
        for (Subsegment subsegment : toRemove) {
            parentSegment.removeSubsegment(subsegment);
        }
    }

    // Benchmark state that initializes a parent segment to operate on. Single level here
    // represents that there is a single level of children after the parent segment.
    @State(Scope.Thread)
    public static class SingleLevelSegmentState {

        // Segments which have a certain number of subsegments.
        public Segment emptySegment;
        public Segment oneChildSegment;
        public Segment twoChildSegment;
        public Segment threeChildSegment;
        public Segment fourChildSegment;

        // X-Ray Recorder
        public AWSXRayRecorder recorder;

        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            recorder = AWSXRayRecorderBuilder.defaultRecorder();
        }

        @Setup(Level.Iteration)
        public void setup() {
            emptySegment = generateSegmentWithChildren(0);
            oneChildSegment = generateSegmentWithChildren(1);
            twoChildSegment = generateSegmentWithChildren(2);
            threeChildSegment = generateSegmentWithChildren(3);
            fourChildSegment = generateSegmentWithChildren(4);
        }

        @TearDown(Level.Iteration)
        public void doTearDown() {
            clearSegment(emptySegment);
            clearSegment(oneChildSegment);
            clearSegment(twoChildSegment);
            clearSegment(threeChildSegment);
            clearSegment(fourChildSegment);
        }

        // Generates a segment with n number of children,
        // Populates the given Entity with annotations, metadata, and other information specific to
        private Segment generateSegmentWithChildren(int numChildren) {
            Segment parentSegment = new SegmentImpl(recorder, SEGMENT_NAME);
            populateEntity(parentSegment);
            for (int i = 0; i < numChildren; i++) {
                Subsegment subsegment = new SubsegmentImpl(recorder, SUBSEGMENT_NAME, parentSegment);
                populateEntity(subsegment);
                subsegment.setParentSegment(parentSegment);
                parentSegment.addSubsegment(subsegment);
            }
            return parentSegment;
        }
    }

    // Benchmark state that initializes a parent segment to operate on. Single level here
    // represents that there is a single level of children after the parent segment.
    @State(Scope.Thread)
    public static class MultiLevelSegmentState {

        // Segments which have a certain depth of children.
        // Two in this case represents Segment -> Subsegment -> Subsegment
        public Segment twoLevelSegment;
        public Segment threeLevelSegment;
        public Segment fourLevelSegment;

        // X-Ray Recorder
        public AWSXRayRecorder recorder;

        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            recorder = AWSXRayRecorderBuilder.defaultRecorder();
        }

        @Setup(Level.Iteration)
        public void setup() {
            twoLevelSegment = generateSegmentWithDepth(2);
            threeLevelSegment = generateSegmentWithDepth(3);
            fourLevelSegment = generateSegmentWithDepth(4);
        }

        @TearDown(Level.Iteration)
        public void doTearDown() {
            clearSegment(twoLevelSegment);
            clearSegment(threeLevelSegment);
            clearSegment(fourLevelSegment);
        }

        // Generates a segment with n number of children,
        // Populates the given Entity with annotations, metadata, and other information specific to
        private Segment generateSegmentWithDepth(int numLevels) {
            Segment parentSegment = new SegmentImpl(recorder, SEGMENT_NAME);
            populateEntity(parentSegment);

            Entity currentEntity = parentSegment;
            for (int i = 0; i < numLevels; i++) {
                Subsegment subsegment = new SubsegmentImpl(recorder, SUBSEGMENT_NAME, parentSegment);
                populateEntity(subsegment);
                currentEntity.addSubsegment(subsegment);
                subsegment.setParentSegment(parentSegment);
                subsegment.setParent(currentEntity);
                currentEntity = subsegment;
            }

            return parentSegment;
        }
    }

    // Serialize a segment with no child subsegments
    @Benchmark
    public String serializeZeroChildSegment(SingleLevelSegmentState state) {
        return state.emptySegment.serialize();
    }

    // Serialize a segment with one child subsegment
    @Benchmark
    public String serializeOneChildSegment(SingleLevelSegmentState state) {
        return state.oneChildSegment.serialize();
    }

    // Serialize a segment with two child subsegments
    @Benchmark
    public String serializeTwoChildSegment(SingleLevelSegmentState state) {
        return state.twoChildSegment.serialize();
    }

    // Serialize a segment with three child subsegments
    @Benchmark
    public String serializeThreeChildSegment(SingleLevelSegmentState state) {
        return state.threeChildSegment.serialize();
    }

    // Serialize a segment with three child subsegments
    @Benchmark
    public String serializeFourChildSegment(SingleLevelSegmentState state) {
        return state.fourChildSegment.serialize();
    }

    // Serialize a segment with two generations of subsegments.
    @Benchmark
    public String serializeTwoGenerationSegment(MultiLevelSegmentState state) {
        return state.twoLevelSegment.serialize();
    }

    // Serialize a segment with three generations of subsegments.
    @Benchmark
    public String serializeThreeGenerationSegment(MultiLevelSegmentState state) {
        return state.threeLevelSegment.serialize();
    }

    // Serialize a segment with four generations of subsegments.
    @Benchmark
    public String serializeFourGenerationSegment(MultiLevelSegmentState state) {
        return state.fourLevelSegment.serialize();
    }
}
