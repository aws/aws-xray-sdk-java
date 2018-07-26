package com.amazonaws.xray.config;

import com.amazonaws.xray.entities.StringValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class DaemonConfiguration {
    private static final Log logger = LogFactory.getLog(DaemonConfiguration.class);
    private static final int DEFAULT_PORT = 2000;
    private static final String DEFAULT_ADDRESS = "127.0.0.1:2000";

    private String TCPAddress;
    private String UDPAddress;

    public InetSocketAddress address;

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
        address = new InetSocketAddress(InetAddress.getLoopbackAddress(), DEFAULT_PORT);
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);
        if (setUDPAndTCPAddress(environmentAddress)) {
            try {
                parseAndModifyDaemonAddress(getUDPAddress());
            } catch (SecurityException | IllegalArgumentException e) {
                logger.error("Error switching to provided daemon address " + environmentAddress + " set by environment variable " + DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY + ". Using loopback address by default.");
            }
        } else if (StringValidator.isNotNullOrBlank(systemAddress)) {
            try {
                parseAndModifyDaemonAddress(systemAddress);
                logger.info("System property " + DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY + " is set. Emitting to daemon on address " + address.toString());
            } catch (SecurityException | IllegalArgumentException e) {
                logger.error("Error switching to provided daemon address " + systemAddress + " set by system property " + DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY + ". Using loopback address by default.");
            }
        } else {
            setUDPAddress(DEFAULT_ADDRESS);
            setTCPAddress(DEFAULT_ADDRESS);
        }
    }

    /**
     *
     * @param addr
     *      A notation of '127.0.0.1:2000' or 'tcp:127.0.0.1:2000 udp:127.0.0.2:2001' are both acceptable. The former one means UDP and TCP are running at the same address.
     */
    public boolean setUDPAndTCPAddress(String addr) {
        if (StringValidator.isNotNullOrBlank(addr)) {
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
        }
        return false;
    }

    private void parseAndModifyDaemonAddress(String socketAddress) {
        int lastColonIndex = socketAddress.lastIndexOf(':');
        if (-1 == lastColonIndex) {
            throw new IllegalArgumentException("Invalid value for agent address: " + socketAddress + ". Value must be of form \"ip_address:port\".");
        }

        String[] parts = socketAddress.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid value for agent address: " + socketAddress + ". Value must be of form \"ip_address:port\".");
        }
        address = new InetSocketAddress(socketAddress.substring(0, lastColonIndex), Integer.parseInt(socketAddress.substring(lastColonIndex + 1, socketAddress.length())));
    }

    public void setTCPAddress(String addr) {
        logger.debug("TCPAddress is set to " + addr + ".");
        this.TCPAddress = addr;
    }

    public String getTCPAddress() {
        return TCPAddress;
    }

    public void setUDPAddress(String addr) {
        logger.debug("UDPAddress is set to " + addr + ".");
        this.UDPAddress = addr;
    }

    public String getUDPAddress() {
        return UDPAddress;
    }

    public String getEndpointForTCPConnection() {
        String[] parts = getTCPAddress().split(":");
        if (parts.length != 2) {
            return "http://" + DEFAULT_ADDRESS;
        }
        return  "http://" + getTCPAddress();
    }
}
