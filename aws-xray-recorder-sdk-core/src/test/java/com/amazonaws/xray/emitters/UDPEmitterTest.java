package com.amazonaws.xray.emitters;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.DummySegment;
import org.junit.Test;

import java.net.DatagramSocket;
import java.net.SocketException;

import static com.amazonaws.xray.AWSXRay.getGlobalRecorder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class UDPEmitterTest {

    @Test
    public void testCustomAddress() throws SocketException {
        String address = "123.4.5.6:1234";
        DaemonConfiguration config = getDaemonConfiguration(address);

        UDPEmitter emitter = new UDPEmitter(config);

        assertEquals(address, emitter.getUDPAddress());
    }


    @Test
    public void sendingSegmentShouldNotThrowExceptions() throws SocketException {
        DaemonConfiguration config = getDaemonConfiguration("host:1234");
        UDPEmitter emitter = new UDPEmitter(mock(DatagramSocket.class), config);

        final boolean success = emitter.sendSegment(new DummySegment(getGlobalRecorder()));

        assertEquals(false, success);
    }

    protected DaemonConfiguration getDaemonConfiguration(final String address) {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress(address);
        return config;
    }
}
