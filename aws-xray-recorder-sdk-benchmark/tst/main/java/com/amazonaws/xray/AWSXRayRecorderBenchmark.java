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

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class AWSXRayRecorderBenchmark {
    private static final String SEGMENT_NAME = "BENCHMARK_SEGMENT";
    private static final String SUBSEGMENT_NAME = "BENCHMARK_SUBSEGMENT";

    // Base state; generates the default recorder.
    @State(Scope.Thread)
    public static class RecorderState {

        // Recorder which always chooses to sample.
        public AWSXRayRecorder recorder;

        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            recorder = AWSXRayRecorderBuilder.defaultRecorder();
        }
    }

    // State that contains a recorder whose sampling decision is always true.
    // This state automatically populates the X-Ray context with a segment and subsegment.
    public static class PopulatedRecorderState extends RecorderState {

        @Override
        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            super.setupOnce();
        }

        @Setup(Level.Iteration)
        public void setupContext() {
            recorder.beginSegment(SEGMENT_NAME);
            recorder.beginSubsegment(SUBSEGMENT_NAME);
        }

        @TearDown(Level.Iteration)
        public void clearContext() {
            recorder.clearTraceEntity();
        }
    }

    // Begin a segment and end the segment; usually in the case when the sampling decision is true.
    @Benchmark
    public void beginEndSegmentBenchmark(RecorderState state) {
        state.recorder.beginSegment(SEGMENT_NAME);
        state.recorder.endSegment();
    }

    // Begin a segment and end the segment; usually in the case when the sampling decision is false.
    @Benchmark
    public void beginEndDummySegmentBenchmark(RecorderState state) {
        state.recorder.beginNoOpSegment();
        state.recorder.endSegment();
    }

    // Begin a segment, begin a subsegment, end the subsegment, and end the segment,
    // where the sampling decision is to true.
    @Benchmark
    public void beginEndSegmentSubsegmentBenchmark(RecorderState state) {
        state.recorder.beginSegment(SEGMENT_NAME);
        state.recorder.beginSubsegment(SUBSEGMENT_NAME);
        state.recorder.endSubsegment();
        state.recorder.endSegment();
    }

    // Begin a segment, begin a subsegment, end the subsegment, and end the segment,
    // where the sampling decision is not true (and a parent dummy segment is generated).
    @Benchmark
    public void beginEndDummySegmentSubsegmentBenchmark(RecorderState state) {
        state.recorder.beginNoOpSegment();
        state.recorder.beginSubsegment(SUBSEGMENT_NAME);
        state.recorder.endSubsegment();
        state.recorder.endSegment();
    }

    // Get Segment benchmark
    // We have to make sure the state that we choose has an entity already populated.
    @Benchmark
    public Segment getSegmentBenchmark(PopulatedRecorderState state) {
        return state.recorder.getCurrentSegment();
    }

    // Get Subsegment benchmark.
    // We have to make sure the context has a subsegment present.
    @Benchmark
    public Subsegment getSubsegmentBenchmark(PopulatedRecorderState state) {
        return state.recorder.getCurrentSubsegment();
    }

    // Convenience main entry-point
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .addProfiler("gc")
            .include(".*" + AWSXRayRecorderBenchmark.class.getSimpleName() + ".*Dummy.*")
            .build();

        new Runner(opt).run();
    }
}
