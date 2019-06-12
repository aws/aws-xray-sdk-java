package com.amazonaws.xray.strategy.sampling;

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
import org.openjdk.jmh.annotations.Warmup;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public class CentralizedSamplingStrategyBenchmark {

    @State(Scope.Thread)
    public static class DefaultSamplingRulesState {
        private CentralizedSamplingStrategy samplingStrategy;
        private SamplingRequest samplingRequest;

        @Setup(Level.Trial)
        public void setupOnce() {
            samplingStrategy = new CentralizedSamplingStrategy();
            samplingRequest = new SamplingRequest("roleARN", "arn:aws:execute-api:us-east-1:1234566789012:qsxrty/test",
                    "serviceName", "hostName", "someMethod", "https://hostName.com", null, null);
        }
    }

    @State(Scope.Thread)
    public static class NoSampleSamplingRulesState {
        private CentralizedSamplingStrategy samplingStrategy;
        private SamplingRequest samplingRequest;
        private static final URL NO_SAMPLE_RULE = LocalizedSamplingStrategy.class.getResource("/com/amazonaws/xray/strategy/sampling/NoSampleRule.json");

        @Setup(Level.Trial)
        public void setupOnce() {
            samplingStrategy = new CentralizedSamplingStrategy(NO_SAMPLE_RULE);
            samplingRequest = new SamplingRequest("roleARN", "arn:aws:execute-api:us-east-1:1234566789012:qsxrty/test",
                    "serviceName", "hostName", "someMethod", "https://hostName.com", null, null);
        }
    }

    // Benchmark default sampling rules on a sampling Request that matches the rules.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean defaultSamplingRuleBenchmark(DefaultSamplingRulesState state) {
        return state.samplingStrategy.shouldTrace(state.samplingRequest).isSampled();
    }

    // Benchmark a no match sampling rule on a sampling request.
    @Benchmark
    @BenchmarkMode(Mode.All)
    @Fork(value=1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean noSampleSamplingBenchmark(NoSampleSamplingRulesState state) {
        return state.samplingStrategy.shouldTrace(state.samplingRequest).isSampled();
    }
}
