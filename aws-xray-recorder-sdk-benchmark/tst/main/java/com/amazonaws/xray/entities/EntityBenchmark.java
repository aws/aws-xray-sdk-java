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
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@Fork(1)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class EntityBenchmark {
    public static final String SEGMENT_NAME = "BENCHMARK_SEGMENT";
    public static final int N_OPERATIONS = 10000;

    // Benchmark state that initializes a parent segment to operate on.
    @State(Scope.Thread)
    public static class MultiSegmentBenchmarkState {
        /**
         * List of segments that we'll perform operations on individually. For consistency, we need a fresh segment
         * for each invocation in most of these tests, but managing 1 segment at Level.Invocation would cause too
         * much instability within the tests because of the speed of each invoke. So we artificially lengthen the time of
         * each invoke by iterating through a large list of segments instead, which makes using Level.Invocation safer.
         */
        public List<Segment> segments;

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
        public void doSetUp() {
            segments = new ArrayList<>();
            for (int i = 0; i < N_OPERATIONS; i++) {
                segments.add(new SegmentImpl(recorder, SEGMENT_NAME));
            }
        }
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        // X-Ray Recorder
        public AWSXRayRecorder recorder;

        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            recorder = AWSXRayRecorderBuilder.defaultRecorder();
        }
    }

    // Construct a segment
    @Benchmark
    public Segment constructSegmentBenchmark(BenchmarkState state) {
        return new SegmentImpl(state.recorder, SEGMENT_NAME);
    }

    // Construct a subsegment and add it to the parent segment.
    @Benchmark
    @OperationsPerInvocation(N_OPERATIONS)
    public void constructSubsegmentPutInSegmentBenchmark(MultiSegmentBenchmarkState state) {
        for (Segment segment : state.segments) {
            new SubsegmentImpl(state.recorder, SEGMENT_NAME, segment);
        }
    }

    // Add an annotation to a segment
    @Benchmark
    @OperationsPerInvocation(N_OPERATIONS)
    public void putAnnotationBenchmark(MultiSegmentBenchmarkState state) {
        for (Segment segment : state.segments) {
            segment.putAnnotation("Key", "Value");
        }
    }

    // Add metadata to a segment
    @Benchmark
    @OperationsPerInvocation(N_OPERATIONS)
    public void putMetadataBenchmark(MultiSegmentBenchmarkState state) {
        for (Segment segment : state.segments) {
            segment.putMetadata("Key", "Value");
        }
    }

    // Add exception into a segment
    @Benchmark
    @OperationsPerInvocation(N_OPERATIONS)
    public void putExceptionSegmentBenchmark(MultiSegmentBenchmarkState state) {
        for (Segment segment : state.segments){
            segment.addException(state.theException);
        }
    }

    // Convenience main entry-point
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .addProfiler("gc")
            .include(".*" + EntityBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
