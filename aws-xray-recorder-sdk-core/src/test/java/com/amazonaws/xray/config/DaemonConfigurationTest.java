package com.amazonaws.xray.config;

import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runners.MethodSorters;
import org.mockito.internal.util.reflection.Whitebox;

@FixMethodOrder(MethodSorters.JVM)
public class DaemonConfigurationTest {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void testDaemonAddressOverrideEnvironmentVariable() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");

        DaemonConfiguration config = new DaemonConfiguration();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(config, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());

        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testDaemonAddressOverrideSystemProperty() {
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "1.2.3.4:5");

        DaemonConfiguration config = new DaemonConfiguration();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(config, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());
    }

    @Test
    public void testDaemonAddressOverrideEnvironmentVariableOverridesSystemProperty() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "6.7.8.9:10");

        DaemonConfiguration config = new DaemonConfiguration();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(config, "address");

        Assert.assertEquals("1.2.3.4", address.getHostString());
        Assert.assertEquals(5, address.getPort());

        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testParseUDPAddressAndTCPAddress() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "tcp:0.0.0.1:2 udp:0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(config, "address");

        Assert.assertEquals("0.0.0.2", address.getHostString());
        Assert.assertEquals(3, address.getPort());
        Assert.assertEquals("http://0.0.0.1:2", config.getEndpointForTCPConnection());

        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }

    @Test
    public void testInvalidEnvironmentVariableDaemonAddress() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "tcp:0.0.0.1:2udp:0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();
        InetSocketAddress address = (InetSocketAddress) Whitebox.getInternalState(config, "address");

        Assert.assertEquals("localhost", address.getHostString());
        Assert.assertEquals(2000, address.getPort());
        Assert.assertEquals("http://127.0.0.1:2000", config.getEndpointForTCPConnection());

        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
    }
}
