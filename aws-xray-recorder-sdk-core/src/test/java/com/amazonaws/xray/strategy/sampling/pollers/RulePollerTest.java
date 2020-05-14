package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RulePollerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private UnsignedXrayClient client;

    @Test
    public void testPollerShutdown() {
        RulePoller poller = new RulePoller(client, new CentralizedManifest(), Clock.systemUTC());
        poller.start();
        poller.shutdown();

        ScheduledExecutorService executor = poller.getExecutor();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testPollerErrorNotAbruptMainThread() throws InterruptedException {
        when(client.getSamplingRules(any())).thenThrow(new OutOfMemoryError());
        RulePoller poller = new RulePoller(client, new CentralizedManifest(), Clock.systemUTC());
        poller.start();
        //Give the mocked client time (1s) to throw the pre-defined Error
        Thread.sleep(1000L);
        assertTrue(Thread.currentThread().isAlive());
    }
}
