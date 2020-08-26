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

package com.amazonaws.xray.strategy.sampling;

import java.net.URL;
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
public class LocalizedSamplingStrategyBenchmark {

    @State(Scope.Thread)
    public static class DefaultSamplingRulesState {
        private LocalizedSamplingStrategy samplingStrategy;
        private SamplingRequest samplingRequest;

        @Setup(Level.Trial)
        public void setupOnce() {
            samplingStrategy = new LocalizedSamplingStrategy();
            samplingRequest = new SamplingRequest("roleARN", "arn:aws:execute-api:us-east-1:1234566789012:qsxrty/test",
                    "serviceName", "hostName", "someMethod", "https://hostName.com", null, null);
        }
    }

    @State(Scope.Thread)
    public static class NoSampleSamplingRulesState {
        private LocalizedSamplingStrategy samplingStrategy;
        private SamplingRequest samplingRequest;
        private static final URL NO_SAMPLE_RULE = LocalizedSamplingStrategy.class.getResource("/com/amazonaws/xray/strategy/sampling/NoSampleRule.json");

        @Setup(Level.Trial)
        public void setupOnce() {
            samplingStrategy = new LocalizedSamplingStrategy(NO_SAMPLE_RULE);
            samplingRequest = new SamplingRequest("roleARN", "arn:aws:execute-api:us-east-1:1234566789012:qsxrty/test",
                    "serviceName", "hostName", "someMethod", "https://hostName.com", null, null);
        }
    }

    // Benchmark default sampling rules on a sampling Request that matches the rules.
    @Benchmark
    public boolean defaultSamplingRuleBenchmark(DefaultSamplingRulesState state) {
        return state.samplingStrategy.shouldTrace(state.samplingRequest).isSampled();
    }

    // Benchmark a no match sampling rule on a sampling request.
    @Benchmark
    public boolean noSampleSamplingBenchmark(NoSampleSamplingRulesState state) {
        return state.samplingStrategy.shouldTrace(state.samplingRequest).isSampled();
    }

    // Convenience main entry-point
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .addProfiler("gc")
            .include(".*" + LocalizedSamplingStrategyBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
