package com.amazonaws.xray;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;

@FixMethodOrder(MethodSorters.JVM)
public class ConcurrencyTest {

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        LocalizedSamplingStrategy defaultSamplingStrategy = new LocalizedSamplingStrategy();
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).withSamplingStrategy(defaultSamplingStrategy).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testManyThreads() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> {
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
        } catch (InterruptedException e) {
            Assert.fail("Concurrent segment creation failed.");
        } finally {
            pool.shutdownNow();
        }
    }

}
