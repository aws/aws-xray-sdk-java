/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.config;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

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
        assertEquals("localhost", address.getHostString());
    }

    @Test
    public void testEnvironmentConfiguration() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("127.0.0.1", address.getHostString());
    }

    @Test
    public void testPropertyConfiguration() {
        System.setProperty(MetricsDaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "127.0.0.1:16888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:16888", config.getUDPAddress());
        assertEquals(16888, address.getPort());
        assertEquals("127.0.0.1", address.getHostString());
    }

    @Test
    public void testEnvironmentOverridesPropertyConfiguration() {
        System.setProperty(MetricsDaemonConfiguration.DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, "127.0.0.1:16888");
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("127.0.0.1", address.getHostString());
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
        assertEquals("127.0.0.1", address.getHostString());
    }

    @Test
    public void setDaemonAddressFailsWhenEnvironment() {
        environmentVariables.set(MetricsDaemonConfiguration.DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, "127.0.0.1:15888");
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        config.setDaemonAddress("127.0.0.1:16888");
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:15888", config.getUDPAddress());
        assertEquals(15888, address.getPort());
        assertEquals("127.0.0.1", address.getHostString());
    }

    @Test
    public void setDaemonAddressWithNoEnv() {
        MetricsDaemonConfiguration config = new MetricsDaemonConfiguration();
        config.setDaemonAddress("127.0.0.1:16888");
        InetSocketAddress address = config.getAddressForEmitter();
        assertEquals("127.0.0.1:16888", config.getUDPAddress());
        assertEquals(16888, address.getPort());
        assertEquals("127.0.0.1", address.getHostString());
    }
}
