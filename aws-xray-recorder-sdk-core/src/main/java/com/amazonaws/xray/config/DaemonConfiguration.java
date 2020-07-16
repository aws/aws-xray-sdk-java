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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DaemonConfiguration {

    /**
     * Environment variable key used to override the address to which UDP packets will be emitted. Valid values are of the form
     * `ip_address:port`. Takes precedence over any system property, constructor value, or setter value used.
     */
    public static final String DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_DAEMON_ADDRESS";

    /**
     * System property key used to override the address to which UDP packets will be emitted. Valid values are of the form
     * `ip_address:port`. Takes precedence over any constructor or setter value used.
     */
    public static final String DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.emitters.daemonAddress";

    private static final Log logger = LogFactory.getLog(DaemonConfiguration.class);
    private static final int DEFAULT_PORT = 2000;
    private static final String DEFAULT_ADDRESS = "127.0.0.1:2000";

    private String tcpAddress = DEFAULT_ADDRESS;

    @Deprecated
    public InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), DEFAULT_PORT);

    // TODO(anuraaga): Refactor to make the address initialization not rely on an uninitialized object.
    @SuppressWarnings("nullness:method.invocation.invalid")
    public DaemonConfiguration() {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);

        if (setUDPAndTCPAddress(environmentAddress)) {
            logger.info(String.format("Environment variable %s is set. Emitting to daemon on address %s.",
                                      DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, getUdpAddress(address)));
        } else if (setUDPAndTCPAddress(systemAddress)) {
            logger.info(String.format("System property %s is set. Emitting to daemon on address %s.",
                                      DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, getUdpAddress(address)));
        }
    }

    /**
     * Sets the daemon address. If either the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable or
     * {@code com.amazonaws.xray.emitters.daemonAddress} system property are set to a non-empty value, calling this method does
     * nothing.
     *
     * @param socketAddress
     *            A notation of '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001' are both acceptable. The former one
     *            means UDP and TCP are running at the same address.
     *
     * @throws IllegalArgumentException
     *             if {@code socketAddress} does not match the specified format.
     */
    public void setDaemonAddress(String socketAddress) {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNullOrBlank(environmentAddress) && StringValidator.isNullOrBlank(systemAddress)) {
            setUDPAndTCPAddress(socketAddress, false);
        } else {
            logger.info(String.format("Ignoring call to setDaemonAddress as one of %s or %s is set.",
                                      DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY));
        }
    }

    /**
     * Force set daemon address regardless of environment variable or system property.
     * It falls back to the default values if the input is invalid.
     *
     * @param addr
     *      A notation of '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001' are both acceptable. The former one means
     *      UDP and TCP are running at the same address.
     */
    public boolean setUDPAndTCPAddress(@Nullable String addr) {
        return setUDPAndTCPAddress(addr, true);
    }

    private boolean setUDPAndTCPAddress(@Nullable String addr, boolean ignoreInvalid) {
        try {
            processAddress(addr);
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            if (ignoreInvalid) {
                return false;
            } else {
                throw e;
            }
        }
    }

    public void setTCPAddress(String addr) {
        logger.debug("TCPAddress is set to " + addr + ".");
        this.tcpAddress = addr;
    }

    public String getTCPAddress() {
        return tcpAddress;
    }

    public void setUDPAddress(String addr) {
        int lastColonIndex = addr.lastIndexOf(':');
        if (-1 == lastColonIndex) {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr
                                               + ". Value must be of form \"ip_address:port\".");
        }

        String[] parts = addr.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr
                                               + ". Value must be of form \"ip_address:port\".");
        }
        address = new InetSocketAddress(addr.substring(0, lastColonIndex), Integer.parseInt(addr.substring(lastColonIndex + 1)));
        logger.debug("UDPAddress is set to " + addr + ".");
    }

    public String getUDPAddress() {
        return getUdpAddress(address);
    }

    private static String getUdpAddress(InetSocketAddress address) {
        return address.getHostString() + ":" + String.valueOf(address.getPort());
    }

    public InetSocketAddress getAddressForEmitter() {
        return address;
    }

    public String getEndpointForTCPConnection() {
        String[] parts = getTCPAddress().split(":");
        if (parts.length != 2) {
            return "http://" + DEFAULT_ADDRESS;
        }
        return  "http://" + getTCPAddress();
    }

    private void processAddress(@Nullable String addr) {
        if (StringValidator.isNullOrBlank(addr)) {
            throw new IllegalArgumentException("Cannot set null daemon address. Value must be of form \"ip_address:port\".");
        }

        String[] splitStr = addr.split("\\s+");
        if (splitStr.length > 2) {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr
                         + ". Value must be of form \"ip_address:port\" or \"tcp:ip_address:port udp:ip_address:port\".");
        }

        if (splitStr.length == 1) {
            setTCPAddress(addr);
            setUDPAddress(addr);
        } else if (splitStr.length == 2) {
            String[] part1 = splitStr[0].split(":");
            String[] part2 = splitStr[1].split(":");
            if (part1.length != 3 && part2.length != 3) {
                throw new IllegalArgumentException("Invalid value for agent address: " + splitStr[0] + " and " + splitStr[1]
                             + ". Value must be of form \"tcp:ip_address:port udp:ip_address:port\".");
            }

            Map<String, String[]> mapping = new HashMap<>();
            mapping.put(part1[0], part1);
            mapping.put(part2[0], part2);
            String[] tcpInfo = mapping.get("tcp");
            String[] udpInfo = mapping.get("udp");
            if (tcpInfo == null || udpInfo == null) {
                throw new IllegalArgumentException("Invalid value for agent address: " + splitStr[0] + " and " + splitStr[1]
                             + ". Value must be of form \"tcp:ip_address:port udp:ip_address:port\".");
            }

            setTCPAddress(tcpInfo[1] + ":" + tcpInfo[2]);
            setUDPAddress(udpInfo[1] + ":" + udpInfo[2]);
        } else {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr
                    + ". Value must be of form \"ip_address:port\" or \"tcp:ip_address:port udp:ip_address:port\".");
        }
    }
}
