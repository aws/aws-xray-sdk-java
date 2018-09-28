package com.amazonaws.xray.emitters;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPEmitter extends Emitter {
    private static final Log logger = LogFactory.getLog(UDPEmitter.class);

    private DatagramSocket daemonSocket;
    private DaemonConfiguration config;
    private byte[] sendBuffer = new byte[DAEMON_BUF_RECEIVE_SIZE];

    /**
     * Constructs a UDPEmitter. Sets the daemon address to the value of the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable or {@code com.amazonaws.xray.emitters.daemonAddress} system property, if either are set
     * to a non-empty value. Otherwise, points to {@code InetAddress.getLoopbackAddress()} at port {@code 2000}.
     *
     * @throws SocketException
     *             if an error occurs while instantiating a {@code DatagramSocket}.
     *
     */
    public UDPEmitter() throws SocketException {
        config = new DaemonConfiguration();
        try {
            daemonSocket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("Exception while instantiating daemon socket.", e);
            throw e;
        }
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
        return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER + segment.serialize()).getBytes(), segment);
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
        return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER + subsegment.streamSerialize()).getBytes(), subsegment);
    }

    private boolean sendData(byte[] data, Entity entity) {
        DatagramPacket packet = new DatagramPacket(sendBuffer, DAEMON_BUF_RECEIVE_SIZE, config.address);
        packet.setData(data);
        try {
            logger.debug("Sending UDP packet.");
            daemonSocket.send(packet);
        } catch (IOException e) {
            logger.error("Exception while sending entity over UDP: " + entity.prettySerialize(), e);
            return false;
        }
        return true;
    }
}
