package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import java.time.Clock;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertTrue;

public class TargetPollerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private UnsignedXrayClient client;

    @Test
    public void testPollerShutdown() {
        TargetPoller poller = new TargetPoller(
            client, new CentralizedManifest(), Clock.systemUTC());
        poller.start();
        poller.shutdown();


        assertTrue(poller.getExecutor().isShutdown());
    }
}
