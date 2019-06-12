package com.amazonaws.xray;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
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

import java.net.SocketException;
import java.util.concurrent.TimeUnit;

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

        @Setup(Level.Invocation)
        public void setup() {

        }

        @TearDown(Level.Invocation)
        public void doTearDown() {
            recorder.clearTraceEntity();
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

        @Setup(Level.Invocation)
        public void setupContext() {
            recorder.beginSegment(SEGMENT_NAME);
            recorder.beginSubsegment(SUBSEGMENT_NAME);
        }
    }

    // State that contains a recorder whose sampling decision is always false.
    // This state automatically populates the X-Ray context with a segment and subsegment.
    public static class DummyPopulatedRecorderState extends RecorderState {

        @Override
        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            super.setupOnce();
        }

        @Setup(Level.Invocation)
        public void setupContext() {
            recorder.beginDummySegment();

            // Doesn't look like dummy subsegment API is available in the recorder?
            recorder.beginSubsegment(SUBSEGMENT_NAME);
        }
    }

    // State that contains a recorder whose sampling decision is always true.
    // Most other states contain a subsegment within the segment. This one makes sure no subsegment exists.
    public static class SegmentNoChildRecorderState extends RecorderState {
        @Override
        @Setup(Level.Trial)
        public void setupOnce() throws SocketException {
            super.setupOnce();
        }

        @Setup(Level.Invocation)
        public void setupContext() {
            recorder.beginSegment(SEGMENT_NAME);
        }
    }

    // Begin segment; this is the case when the decision is to sample.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Segment beginSegmentBenchmark(RecorderState state) {
        return state.recorder.beginSegment(SEGMENT_NAME);
    }

    // Begin Dummy Segment; this is the case when the sampling decision is to not sample
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Segment beginDummySegmentBenchmark(RecorderState state) {
        return state.recorder.beginDummySegment();
    }

    // End segment for segments that are not sampled (dummy segments).
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endDummySegmentBenchmark(DummyPopulatedRecorderState state) {
        state.recorder.endSegment();
    }

    // End segment for segments that are sampled.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endSegmentBenchmark(SegmentNoChildRecorderState state) {
        state.recorder.endSegment();
    }

    // End segment and child segment for a context which has a prepopulated segment and child subsegment.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endSegmentWithChildBenchmark(PopulatedRecorderState state) {
        state.recorder.endSubsegment();
        state.recorder.endSegment();
    }

    // End segment for a context which has only a prepopulated segment.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endSegmentNoChildBenchmark(SegmentNoChildRecorderState state) {
        state.recorder.endSegment();
    }

    // Begin a segment and end the segment; usually in the case when the sampling decision is true.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void beginEndSegmentBenchmark(RecorderState state) {
        state.recorder.beginSegment(SEGMENT_NAME);
        state.recorder.endSegment();
    }

    // Begin a segment and end the segment; usually in the case when the sampling decision is false.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void beginEndDummySegmentBenchmark(RecorderState state) {
        state.recorder.beginDummySegment();
        state.recorder.endSegment();
    }

    // Begin a segment in a context that has a segment already where sampling occurs.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Subsegment beginSubsegmentBenchmark(SegmentNoChildRecorderState state) {
        return state.recorder.beginSubsegment(SUBSEGMENT_NAME);
    }

    // Begin a subsegment in a context that has a segment where the sampling decision is false.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Subsegment beginSubsegmentDummyParentBenchmark(DummyPopulatedRecorderState state) {
        return state.recorder.beginSubsegment(SUBSEGMENT_NAME);
    }

    // End subsegment for subsegments that are not sampled.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endSubsegmentDummyParentBenchmark(DummyPopulatedRecorderState state) {
        state.recorder.endSubsegment();
    }

    // End segment for segments that are sampled
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void endSubsegmentBenchmark(PopulatedRecorderState state) {
        state.recorder.endSubsegment();
    }

    // Begin a segment, begin a subsegment, end the subsegment, and end the segment,
    // where the sampling decision is to true.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void beginEndSegmentSubsegmentBenchmark(RecorderState state) {
        state.recorder.beginSegment(SEGMENT_NAME);
        state.recorder.beginSubsegment(SUBSEGMENT_NAME);
        state.recorder.endSubsegment();
        state.recorder.endSegment();
    }

    // Begin a segment, begin a subsegment, end the subsegment, and end the segment,
    // where the sampling decision is not true (and a parent dummy segment is generated).
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void beginEndDummySegmentSubsegmentBenchmark(RecorderState state) {
        state.recorder.beginDummySegment();
        state.recorder.beginSubsegment(SUBSEGMENT_NAME);
        state.recorder.endSubsegment();
        state.recorder.endSegment();
    }

    // Get Segment benchmark
    // We have to make sure the state that we choose has an entity already populated.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Segment getSegmentBenchmark(PopulatedRecorderState state) {
        return state.recorder.getCurrentSegment();
    }

    // Get Subsegment benchmark.
    // We have to make sure the context has a subsegment present.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Subsegment getSubsegmentBenchmark(PopulatedRecorderState state) {
        return state.recorder.getCurrentSubsegment();
    }
}
