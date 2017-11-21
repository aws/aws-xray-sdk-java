package com.amazonaws.xray.emitters;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.StringValidator;
import com.amazonaws.xray.entities.Subsegment;

public class UDPEmitter extends Emitter {
    private static final Log logger = LogFactory.getLog(UDPEmitter.class);

    private static final int DEFAULT_PORT = 2000;

    private DatagramSocket daemonSocket;
    private InetSocketAddress address;
    private byte[] sendBuffer = new byte[DAEMON_BUF_RECEIVE_SIZE];

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

    /**
     * Constructs a UDPEmitter. Sets the daemon address to the value of the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable or {@code com.amazonaws.xray.emitters.daemonAddress} system property, if either are set
     * to a non-empty value. Otherwise, points to {@code InetAddress.getLoopbackAddress()} at port {@code 2000}.
     *
     * @throws SocketException
     *             if an error occurs while instantiating a {@code DatagramSocket}.
     *
     */
    public UDPEmitter() throws SocketException {
        try {
            daemonSocket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("Exception while instantiating daemon socket.", e);
            throw e;
        }

        address = new InetSocketAddress(InetAddress.getLoopbackAddress(), DEFAULT_PORT);
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNotNullOrBlank(environmentAddress)) {
            try {
                parseAndModifyDaemonAddress(environmentAddress);
                logger.info("Environment variable " + DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY + " is set. Emitting to daemon on address " + address.toString());
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
        }

    }

    /**
     * Sets the daemon address. If either the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable or {@code com.amazonaws.xray.emitters.daemonAddress} system property are set to a non-empty value, calling this method does nothing.
     *
     * @param socketAddress
     *            Daemon address and port in the "ip_address:port" format.
     *
     * @throws IllegalArgumentException
     *             if {@code socketAddress} does not match the specified format.
     */
    public void setDaemonAddress(String socketAddress) {
        String environmentAddress = System.getenv(DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY);
        String systemAddress = System.getProperty(DAEMON_ADDRESS_SYSTEM_PROPERTY_KEY);
        if (StringValidator.isNullOrBlank(environmentAddress) && StringValidator.isNullOrBlank(systemAddress)) {
            parseAndModifyDaemonAddress(socketAddress);
        } else {
            logger.info("Ignoring call to setDaemonAddress as " + DAEMON_ADDRESS_ENVIRONMENT_VARIABLE_KEY + " is set.");
        }
    }

    private void parseAndModifyDaemonAddress(String socketAddress) {
        int lastColonIndex = socketAddress.lastIndexOf(':');
        if (-1 == lastColonIndex) {
            throw new IllegalArgumentException("Invalid value for agent address: " + socketAddress + ". Value must be of form \"ip_address:port\".");
        }
        address = new InetSocketAddress(socketAddress.substring(0, lastColonIndex), Integer.parseInt(socketAddress.substring(lastColonIndex + 1, socketAddress.length())));
    }

    /**
     * {@inheritDoc}
     *
     * @see Emitter#sendSegment(Segment)
     */
    public boolean sendSegment(Segment segment) {
        if (logger.isDebugEnabled()) {
            logger.debug(segment.prettySerialize());
        }
        return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER + segment.serialize()).getBytes());
    }

    /**
     * {@inheritDoc}
     *
     * @see Emitter#sendSubsegment(Subsegment)
     */
    public boolean sendSubsegment(Subsegment subsegment) {
        if (logger.isDebugEnabled()) {
            logger.debug(subsegment.prettyStreamSerialize());
        }
        return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER + subsegment.streamSerialize()).getBytes());
    }

    private boolean sendData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(sendBuffer, DAEMON_BUF_RECEIVE_SIZE, address);
        packet.setData(data);
        try {
            logger.debug("Sending UDP packet.");
            daemonSocket.send(packet);
        } catch (IOException e) {
            logger.error("Exception while sending segment over UDP.", e);
            return false;
        }
        return true;
    }
}
