package com.amazonaws.xray;

import java.util.concurrent.ForkJoinPool;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import com.amazonaws.xray.emitters.Emitter;

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

    private ForkJoinPool pool = new ForkJoinPool();

    @Test
    public void testManyThreads() {
        for (int i = 0; i < 1000; i++) {
            pool.submit(() -> {
                AWSXRay.createSegment("test", (segment) -> {
                    AWSXRay.createSubsegment("test", (subsegment) -> {
                        AWSXRay.createSubsegment("a", (aSubsegment) -> {
                        });
                    });
                });
            });
        }
    }

}
