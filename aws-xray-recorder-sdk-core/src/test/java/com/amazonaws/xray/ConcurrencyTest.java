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
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConcurrencyTest {

    @BeforeEach
    void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter)
                                                        .withSamplingStrategy(defaultSamplingStrategy).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testManyThreads() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            pool.execute(() -> {
                try {
                    AWSXRay.createSegment("Segment", (segment) -> {
                        AWSXRay.createSubsegment("Subsegment", (subsegment) -> {
                            AWSXRay.createSubsegment("NestedSubsegment", (nSubsegment) -> {
                            });
                        });
                    });
                    latch.countDown();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

}
