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

import com.amazonaws.xray.ThreadLocalStorage;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(1)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(16)
public class IdsBenchmark {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final TraceID TRACE_ID = TraceID.create();

    @Benchmark
    public TraceID traceId_create() {
        return TraceID.create();
    }

    @Benchmark
    public TraceID traceId_parse() {
        return TraceID.fromString("1-57ff426a-80c11c39b0c928905eb0828d");
    }

    @Benchmark
    public String traceId_serialize() {
        return TRACE_ID.toString();
    }

    @Benchmark
    public BigInteger traceId_secureRandom() {
        return new BigInteger(96, SECURE_RANDOM);
    }

    @Benchmark
    public BigInteger traceId_threadLocalSecureRandom() {
        return new BigInteger(96, ThreadLocalStorage.getRandom());
    }

    @Benchmark
    public BigInteger traceId_threadLocalRandom() {
        return new BigInteger(96, ThreadLocalRandom.current());
    }

    @Benchmark
    public String segmentId_secureRandom() {
        return Long.toString(SECURE_RANDOM.nextLong() >>> 1, 16);
    }

    @Benchmark
    public String segmentId_threadLocalSecureRandom() {
        return Long.toString(ThreadLocalStorage.getRandom().nextLong() >>> 1, 16);
    }

    @Benchmark
    public String segmentId_threadLocalRandom() {
        return Long.toString(ThreadLocalRandom.current().nextLong() >>> 1, 16);
    }

    // Convenience main entry-point
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .addProfiler("gc")
            .include(".*" + IdsBenchmark.class.getSimpleName() + ".*_(create|parse|serialize)")
            .build();

        new Runner(opt).run();
    }
}
