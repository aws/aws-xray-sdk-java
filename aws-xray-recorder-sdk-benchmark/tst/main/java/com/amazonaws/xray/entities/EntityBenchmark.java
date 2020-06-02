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

public class EntityBenchmark {
    public static final String SEGMENT_NAME = "BENCHMARK_SEGMENT";

    // Benchmark state that initializes a parent segment to operate on.
    @State(Scope.Thread)
    public static class BenchmarkState {

        // Recorder which always chooses to sample.
        public Segment parentSegment;

        // X-Ray Recorder
        public AWSXRayRecorder recorder;

        // An exception object
        public Exception theException;

        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            recorder = AWSXRayRecorderBuilder.defaultRecorder();
            theException = new Exception("Test Exception");
        }

        @Setup(Level.Invocation)
        public void setup() {
            parentSegment = new SegmentImpl(recorder, SEGMENT_NAME);
        }

        @TearDown(Level.Invocation)
        public void doTearDown() {
            for (Subsegment subsegment : parentSegment.getSubsegments()) {
                parentSegment.removeSubsegment(subsegment);
            }
        }
    }

    // Construct a segment
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Segment constructSegmentBenchmark(BenchmarkState state) {
        return new SegmentImpl(state.recorder, SEGMENT_NAME);
    }

    // Construct a subsegment and add it to the parent segment.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Subsegment constructSubsegmentPutInSegmentBenchmark(BenchmarkState state) {
        // TODO: Find a way to create just the subsegment and not force it into the parent segment?
        return new SubsegmentImpl(state.recorder, SEGMENT_NAME, state.parentSegment);
    }

    // Add an annotation to a segment
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void putAnnotationBenchmark(BenchmarkState state) {
        state.parentSegment.putAnnotation("Key", "Value");
    }

    // Add metadata to a segment
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void putMetadataBenchmark(BenchmarkState state) {
        state.parentSegment.putMetadata("Key", "Value");
    }

    // Add exception into a segment
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void putExceptionSegmentBenchmark(BenchmarkState state) {
        state.parentSegment.addException(state.theException);
    }
}
