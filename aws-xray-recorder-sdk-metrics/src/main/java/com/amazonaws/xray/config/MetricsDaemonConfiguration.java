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

import com.amazonaws.xray.entities.StringValidator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configuration specifying where to publish EMF metrics over UDP
 */
public class MetricsDaemonConfiguration {

    /**
     * Environment variable key used to override the address to which UDP packets will be emitted. Valid values are of the form
     * `ip_address:port`. Takes precedence over the system property when used.
     */
    public static final String DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_METRICS_DAEMON_ADDRESS";

    /**
     * System property key used to override the address to which UDP packets will be emitted. Valid values are of the form
     * `ip_address:port`.
     * used.
     */
    public static final String DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.metrics.daemonAddress";

    private static final Log logger = LogFactory.getLog(MetricsDaemonConfiguration.class);
    private static final int DEFAULT_PORT = 25888;

    private InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), DEFAULT_PORT);

    public MetricsDaemonConfiguration() {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);

        if (setUDPAddress(environmentAddress)) {
            logger.info(String.format("Environment variable %s is set. Emitting to daemon on address %s.",
                                      DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, getUDPAddress()));
        } else if (setUDPAddress(systemAddress)) {
            logger.info(String.format("System property %s is set. Emitting to daemon on address %s.",
                                      DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, getUDPAddress()));
        }
    }

    /**
     * Sets the metrics daemon address. If either the {@code AWS_XRAY_METRICS_DAEMON_ADDRESS} environment variable or
     * {@code com.amazonaws.xray.metrics.daemonAddress} system property are set to a non-empty value, calling this method does
     * nothing. Logs an error if the address format is invalid to allow tracing if metrics are inoperative.
     *
     * @param socketAddress
     *            Formatted as '127.0.0.1:25888'
     */
    public void setDaemonAddress(String socketAddress) {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNullOrBlank(environmentAddress) && StringValidator.isNullOrBlank(systemAddress)) {
            setUDPAddress(socketAddress);
        } else {
            logger.info(String.format("Ignoring call to setDaemonAddress as one of %s or %s is set.",
                                      DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY));
        }
    }

    /**
     * Set metrics daemon address, ignoring the value of of environment variable or system property.
     * Logs an error if the address format is invalid to allow tracing if metrics are inoperative.
     *
     * @param addr
     *      Formatted as '127.0.0.1:25888'
     *
     * @return true if the address updates without error
     */
    public boolean setUDPAddress(String addr) {
        if (addr == null) {
            return false;
        }

        int lastColonIndex = addr.lastIndexOf(':');
        if (-1 == lastColonIndex) {
            logger.error("Invalid value for agent address: " + addr + ". Value must be of form \"ip_address:port\".");
            return false;
        }

        String[] parts = addr.split(":");
        if (parts.length != 2) {
            logger.error("Invalid value for agent address: " + addr + ". Value must be of form \"ip_address:port\".");
            return false;
        }
        address = new InetSocketAddress(addr.substring(0, lastColonIndex), Integer.parseInt(addr.substring(lastColonIndex + 1)));
        logger.debug("UDPAddress is set to " + addr + ".");

        return true;
    }

    /**
     * Get the UDP address to publish metrics to.
     * @return the address in string form
     */
    public String getUDPAddress() {
        return address.getHostString() + ":" + String.valueOf(address.getPort());
    }

    /**
     * Get the socket address to publish metrics to.
     * @return the address as InetSocketAddress
     */
    public InetSocketAddress getAddressForEmitter() {
        return address;
    }
}
