package com.amazonaws.xray.strategy.sampling.pollers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;

public class TargetPollerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private CentralizedManifest manifest;

    @Mock
    private UnsignedXrayClient client;

    @Test
    public void testPollerShutdown() {
        TargetPoller poller = new TargetPoller(client, manifest, Clock.systemUTC());
        poller.start();
        poller.shutdown();

        assertThat(poller.getExecutor().isShutdown()).isTrue();
    }
}
