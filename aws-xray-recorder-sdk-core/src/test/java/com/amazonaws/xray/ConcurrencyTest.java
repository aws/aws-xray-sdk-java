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
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.SegmentImpl;
import com.amazonaws.xray.entities.SubsegmentImpl;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Callable;
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

    @Test
    public void testPrecursorIdConcurrency() throws Exception {
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        SubsegmentImpl subseg = new SubsegmentImpl(AWSXRay.getGlobalRecorder(), "test", seg);

        final long startTime = System.currentTimeMillis();

        Thread thread1 = new Thread(() -> {
            while (true) {
                subseg.addPrecursorId("ID" + new Random().nextInt());
            }
        });
        thread1.start();

        Callable<Void> callable = (Callable) () -> {
            while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(1)) {
                Iterator it = subseg.getPrecursorIds().iterator();
                while (it.hasNext()) {
                    it.next();
                }
            }
            return null;
        };
        callable.call();

        thread1.join();
    }
}
