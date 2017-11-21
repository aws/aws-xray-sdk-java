package com.amazonaws.xray.emitters;

import java.net.InetSocketAddress;
import java.net.SocketException;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runners.MethodSorters;
import org.mockito.internal.util.reflection.Whitebox;

@FixMethodOrder(MethodSorters.JVM)
public class UDPEmitterTest {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void testDaemonAddressOverrideEnvironmentVariable() throws SocketException {
        environmentVariables.set(UDPEmitter.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");

        UDPEmitter emitter = new UDPEmitter();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(emitter, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());

        environmentVariables.set(UDPEmitter.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testDaemonAddressOverrideSystemProperty() throws SocketException {
        System.setProperty(UDPEmitter.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "1.2.3.4:5");

        UDPEmitter emitter = new UDPEmitter();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(emitter, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());
    }

    @Test
    public void testDaemonAddressOverrideEnvironmentVariableOverridesSystemProperty() throws SocketException {
        environmentVariables.set(UDPEmitter.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");
        System.setProperty(UDPEmitter.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "6.7.8.9:10");

        UDPEmitter emitter = new UDPEmitter();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(emitter, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());

        environmentVariables.set(UDPEmitter.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }
}
