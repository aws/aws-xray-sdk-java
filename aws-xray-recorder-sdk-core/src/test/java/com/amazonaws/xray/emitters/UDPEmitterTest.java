package com.amazonaws.xray.emitters;

import static com.amazonaws.xray.AWSXRay.getGlobalRecorder;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.DummySegment;
import java.net.SocketException;
import org.junit.Test;

public class UDPEmitterTest {

    @Test
    public void testCustomAddress() throws SocketException {
        String address = "123.4.5.6:1234";
        DaemonConfiguration config = getDaemonConfiguration(address);

        UDPEmitter emitter = new UDPEmitter(config);

        assertThat(emitter.getUDPAddress()).isEqualTo(address);
    }


    @Test
    public void sendingSegmentShouldNotThrowExceptions() throws SocketException {
        DaemonConfiguration config = getDaemonConfiguration("__udpemittertest_unresolvable__:1234");
        UDPEmitter emitter = new UDPEmitter(config);

        boolean success = emitter.sendSegment(new DummySegment(getGlobalRecorder()));
        assertThat(success).isFalse();
    }

    protected DaemonConfiguration getDaemonConfiguration(final String address) {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress(address);
        return config;
    }
}
