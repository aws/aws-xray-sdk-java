package com.amazonaws.xray.config;

import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class DaemonConfigurationTest {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setUp() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "");
    }

    @Test
    public void testDefaultDaemonConfiguration() {
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("localhost", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(2000, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://127.0.0.1:2000", config.getEndpointForTCPConnection());
    }

    @Test
    public void testDaemonAddressEnvironmentVariableOverride() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("1.2.3.4", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(5, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://1.2.3.4:5", config.getEndpointForTCPConnection());
    }

    @Test
    public void testDaemonAddressSystemPropertyOverride() {
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "1.2.3.4:5");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("1.2.3.4", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(5, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://1.2.3.4:5", config.getEndpointForTCPConnection());
    }

    @Test
    public void testDaemonAddressEnvironmentVariableOverridesSystemProperty() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "1.2.3.4:5");
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "6.7.8.9:10");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("1.2.3.4", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(5, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://1.2.3.4:5", config.getEndpointForTCPConnection());
    }

    @Test
    public void testDaemonAddressSystemPropertyOverridesRuntimeConfig() {
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "1.2.3.4:5");
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress("6.7.8.9:10");
    }

    @Test
    public void testDaemonAddressConfigAtRuntime() {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress("1.2.3.4:5");

        Assert.assertEquals("1.2.3.4", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(5, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://1.2.3.4:5", config.getEndpointForTCPConnection());
    }

    @Test
    public void testSetUDPAddressAndTCPAddressThroughEnvVar() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "tcp:0.0.0.1:2 udp:0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("0.0.0.2", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(3, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://0.0.0.1:2", config.getEndpointForTCPConnection());
    }

    @Test
    public void testSetUDPAddressAndTCPAddressThroughSystemProperty() {
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "tcp:0.0.0.1:2 udp:0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("0.0.0.2", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(3, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://0.0.0.1:2", config.getEndpointForTCPConnection());
    }

    @Test
    public void testSetUDPAddressAndTCPAddressAtRuntime() {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress("udp:0.0.0.2:3 tcp:0.0.0.1:2");

        Assert.assertEquals("0.0.0.2", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(3, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://0.0.0.1:2", config.getEndpointForTCPConnection());
    }

    @Test
    public void testShouldNotThrowOnInvalidEnvVar() {
        environmentVariables.set(DaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "tcp:0.0.0.1:2udp:0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("localhost", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(2000, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://127.0.0.1:2000", config.getEndpointForTCPConnection());
    }

    @Test
    public void testShouldNotThrowOnInvalidSystemProperty() {
        System.setProperty(DaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "tcp0.0.0.1:2 udp0.0.0.2:3");
        DaemonConfiguration config = new DaemonConfiguration();

        Assert.assertEquals("localhost", config.getAddressForEmitter().getHostString());
        Assert.assertEquals(2000, config.getAddressForEmitter().getPort());
        Assert.assertEquals("http://127.0.0.1:2000", config.getEndpointForTCPConnection());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testShouldThrowOnInvalidRuntimeConfig() {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress("0.0.0.1");
    }
}
