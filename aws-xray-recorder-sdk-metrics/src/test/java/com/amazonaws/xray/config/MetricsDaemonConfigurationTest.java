package com.amazonaws.xray.config;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;

public class MetricsDaemonConfigurationTest {
    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    @Rule
    public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setup() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, null);
        System.setProperty(MetricsDaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "");
    }

    @Test
    public void testDefaultConfiguration() {
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("localhost:25888", config.getUDPAddress());
        assertEquals(25888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void testEnvironmentConfiguration() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void testPropertyConfiguration() {
        System.setProperty(MetricsDaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "127.0.0.1:16888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:16888", config.getUDPAddress());
        assertEquals(16888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void testEnvironmentOverridesPropertyConfiguration() {
        System.setProperty(MetricsDaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "127.0.0.1:16888");
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void invalidFormat() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        config.setUDPAddress("INVALID_STRING");
        config.setUDPAddress("REALLY:INVALID:STRING");
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void setDaemonAddressFailsWhenEnvironment() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        config.setDaemonAddress("127.0.0.1:16888");
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }

    @Test
    public void setDaemonAddressWithNoEnv() {
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        config.setDaemonAddress("127.0.0.1:16888");
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:16888", config.getUDPAddress());
        assertEquals(16888, address.getPort());
        assertEquals("localhost", address.getAddress().getHostName());
    }
}
