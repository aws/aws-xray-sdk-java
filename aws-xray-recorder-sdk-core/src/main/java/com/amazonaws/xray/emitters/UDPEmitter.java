package com.amazonaws.xray.emitters;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

/**
 * @deprecated Use {@link Emitter#create()}.
 */
@Deprecated
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
        this(new DaemonConfiguration());
    }

    /**
     * Constructs a UDPEmitter. This overload allows you to specify the configuration.
     *
     * @param config The {@link DaemonConfiguration} for the Emitter.
     * @throws SocketException
     *             if an error occurs while instantiating a {@code DatagramSocket}.
     */
    public UDPEmitter(DaemonConfiguration config) throws SocketException {
        this.config = config;
        try {
            daemonSocket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("Exception while instantiating daemon socket.", e);
            throw e;
        }
    }

    public String getUDPAddress() {
        return config.getUDPAddress();
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
        DatagramPacket packet = new DatagramPacket(sendBuffer, DAEMON_BUF_RECEIVE_SIZE, config.getAddressForEmitter());
        packet.setData(data);
        try {
            logger.debug("Sending UDP packet.");
            daemonSocket.send(packet);
        } catch (IOException e) {
            String segmentName = Optional.ofNullable(entity.getParent()).map(this::nameAndId).orElse("[no parent segment]");
            logger.error("Exception while sending segment over UDP for entity " +  nameAndId(entity) + " on segment " + segmentName, e);
            return false;
        }
        return true;
    }

    private String nameAndId(Entity entity) {
        return entity.getName() + " [" + entity.getId() + "]";
    }
}
