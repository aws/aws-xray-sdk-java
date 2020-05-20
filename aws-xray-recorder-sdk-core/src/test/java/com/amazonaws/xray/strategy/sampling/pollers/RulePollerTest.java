package com.amazonaws.xray.strategy.sampling.pollers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;

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
        assertThat(executor.isShutdown()).isTrue();
    }
}
