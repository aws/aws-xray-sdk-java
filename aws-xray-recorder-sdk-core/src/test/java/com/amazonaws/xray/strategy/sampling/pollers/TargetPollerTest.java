package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.services.xray.AWSXRayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

public class TargetPollerTest {

    @Test
    public void testPollerShutdown() {
        TargetPoller poller = new TargetPoller(new CentralizedManifest(), getMockedClient(), Clock.systemUTC());
        poller.start();
        poller.shutdown();


        Assert.assertTrue(getExecutor(poller).isShutdown());
    }

    private ScheduledExecutorService getExecutor(TargetPoller poller) {
        return (ScheduledExecutorService) Whitebox.getInternalState(poller, "executor");
    }

    private AWSXRayClient getMockedClient() {
        return Mockito.mock(AWSXRayClient.class);
    }
}
