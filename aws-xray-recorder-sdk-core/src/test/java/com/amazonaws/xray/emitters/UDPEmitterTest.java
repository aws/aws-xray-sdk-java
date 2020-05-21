package com.amazonaws.xray.emitters;

import com.amazonaws.xray.config.DaemonConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.net.SocketException;

public class UDPEmitterTest {

    @Test
    public void testCustomAddress() throws SocketException {
        String address = "123.4.5.6:1234";
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress(address);

        UDPEmitter emitter = new UDPEmitter(config);

        Assert.assertEquals(address, emitter.getUDPAddress());
    }
}
