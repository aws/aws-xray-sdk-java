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

public class DaemonConfiguration {
    private static final Log logger = LogFactory.getLog(DaemonConfiguration.class);
    private static final int DEFAULT_PORT = 2000;
    private static final String DEFAULT_ADDRESS = "127.0.0.1:2000";

    private String TCPAddress = DEFAULT_ADDRESS;

    @Deprecated
    public InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), DEFAULT_PORT);

    /**
     * Environment variable key used to override the address to which UDP packets will be emitted. Valid values are of the form `ip_address:port`. Takes precedence over any system property,
     * constructor value, or setter value used.
     */
    public static final String DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY = "AWS_XRAY_DAEMON_ADDRESS";

    /**
     * System property key used to override the address to which UDP packets will be emitted. Valid values are of the form `ip_address:port`. Takes precedence over any constructor or setter value
     * used.
     */
    public static final String DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY = "com.amazonaws.xray.emitters.daemonAddress";

    public DaemonConfiguration() {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);

        if (setUDPAndTCPAddress(environmentAddress)) {
            logger.info(String.format("Environment variable %s is set. Emitting to daemon on address %s.", DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, getUDPAddress()));
        } else if (setUDPAndTCPAddress(systemAddress)) {
            logger.info(String.format("System property %s is set. Emitting to daemon on address %s.", DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY, getUDPAddress()));
        }
    }

    /**
     * Sets the daemon address. If either the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable or {@code com.amazonaws.xray.emitters.daemonAddress} system property are set to a non-empty value, calling this method does nothing.
     *
     * @param socketAddress
     *            A notation of '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001' are both acceptable. The former one means UDP and TCP are running at the same address.
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
            logger.info(String.format("Ignoring call to setDaemonAddress as one of %s or %s is set.", DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY, DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY));
        }
    }

    /**
     * Force set daemon address regardless of environment variable or system property.
     * It falls back to the default values if the input is invalid.
     *
     * @param addr
     *      A notation of '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001' are both acceptable. The former one means UDP and TCP are running at the same address.
     */
    public boolean setUDPAndTCPAddress(String addr) {
        return setUDPAndTCPAddress(addr, true);
    }

    private boolean setUDPAndTCPAddress(String addr, boolean ignoreInvalid) {
        boolean result = false;
        try {
            result = processAddress(addr);
        } catch (SecurityException | IllegalArgumentException e) {
            if (ignoreInvalid) {
                return result;
            } else {
                throw e;
            }
        }

        return result;
    }

    public void setTCPAddress(String addr) {
        logger.debug("TCPAddress is set to " + addr + ".");
        this.TCPAddress = addr;
    }

    public String getTCPAddress() {
        return TCPAddress;
    }

    public void setUDPAddress(String addr) {
        int lastColonIndex = addr.lastIndexOf(':');
        if (-1 == lastColonIndex) {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr + ". Value must be of form \"ip_address:port\".");
        }

        String[] parts = addr.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid value for agent address: " + addr + ". Value must be of form \"ip_address:port\".");
        }
        address = new InetSocketAddress(addr.substring(0, lastColonIndex), Integer.parseInt(addr.substring(lastColonIndex + 1, addr.length())));
        logger.debug("UDPAddress is set to " + addr + ".");
    }

    public String getUDPAddress() {
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

    private boolean processAddress(String addr) {
        if (StringValidator.isNullOrBlank(addr)) {
            return false;
        }

        String[] splitStr = addr.split("\\s+");
        if (splitStr.length > 2) {
            logger.error("Invalid value for agent address: " + addr + ". Value must be of form \"ip_address:port\" or \"tcp:ip_address:port udp:ip_address:port\".");
            return false;
        }

        if (splitStr.length == 1) {
            setTCPAddress(addr);
            setUDPAddress(addr);
            return true;
        }

        if (splitStr.length == 2) {
            String[] part1 = splitStr[0].split(":");
            String[] part2 = splitStr[1].split(":");
            if (part1.length != 3 && part2.length != 3) {
                logger.error("Invalid value for agent address: " + splitStr[0] + " and " + splitStr[1] + ". Value must be of form \"tcp:ip_address:port udp:ip_address:port\".");
                return false;
            }

            Map<String, String[]> mapping = new HashMap<String, String[]>();
            mapping.put(part1[0], part1);
            mapping.put(part2[0], part2);
            String[] TCPInfo = mapping.get("tcp");
            String[] UDPInfo = mapping.get("udp");

            setTCPAddress(TCPInfo[1] + ":" + TCPInfo[2]);
            setUDPAddress(UDPInfo[1] + ":" + UDPInfo[2]);

            return true;
        }

        return false;
    }
}
