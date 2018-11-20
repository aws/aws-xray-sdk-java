package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.services.xray.AWSXRayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

public class RulePollerTest {

    @Test
    public void testPollerShutdown() {
        RulePoller poller = new RulePoller(new CentralizedManifest(), getMockedClient(), Clock.systemUTC());
        poller.start();
        poller.shutdown();

        ScheduledExecutorService executor = getExecutor(poller);
        Assert.assertTrue(executor.isShutdown());
    }

    @Test
    public void testPollerErrorNotAbruptMainThread() throws InterruptedException {
        AWSXRayClient mockedClient = getMockedClient();
        Mockito.doThrow(new NoSuchMethodError()).when(mockedClient).getSamplingRules(Mockito.any());
        RulePoller poller = new RulePoller(new CentralizedManifest(), mockedClient, Clock.systemUTC());
        poller.start();
        //Give the mocked client time (1s) to throw the pre-defined Error
        Thread.sleep(1000L);
        Assert.assertTrue(Thread.currentThread().isAlive());
    }

    private ScheduledExecutorService getExecutor(RulePoller poller) {
        return (ScheduledExecutorService) Whitebox.getInternalState(poller, "executor");
    }

    private AWSXRayClient getMockedClient() {
        return Mockito.mock(AWSXRayClient.class);
    }
}
