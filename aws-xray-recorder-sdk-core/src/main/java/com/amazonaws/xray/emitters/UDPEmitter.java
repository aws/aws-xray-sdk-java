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

package com.amazonaws.xray.emitters;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
     * Constructs a UDPEmitter. Sets the daemon address to the value of the {@code AWS_XRAY_DAEMON_ADDRESS} environment variable
     * or {@code com.amazonaws.xray.emitters.daemonAddress} system property, if either are set to a non-empty value. Otherwise,
     * points to {@code InetAddress.getLoopbackAddress()} at port {@code 2000}.
     *
     * @throws SocketException
     *             if an error occurs while instantiating a {@code DatagramSocket}.
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
    @Override
    public boolean sendSegment(Segment segment) {
        if (logger.isDebugEnabled()) {
            logger.debug(segment.prettySerialize());
        }
        if (segment.compareAndSetEmitted(false, true)) {
            return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER + segment.serialize()).getBytes(StandardCharsets.UTF_8),
                            segment);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see Emitter#sendSubsegment(Subsegment)
     */
    @Override
    public boolean sendSubsegment(Subsegment subsegment) {
        if (logger.isDebugEnabled()) {
            logger.debug(subsegment.prettyStreamSerialize());
        }
        if (subsegment.compareAndSetEmitted(false, true)) {
            return sendData((PROTOCOL_HEADER + PROTOCOL_DELIMITER +
                             subsegment.streamSerialize()).getBytes(StandardCharsets.UTF_8),
                            subsegment);
        } else {
            return false;
        }
    }

    private boolean sendData(byte[] data, Entity entity) {
        try {
            DatagramPacket packet = new DatagramPacket(sendBuffer, DAEMON_BUF_RECEIVE_SIZE, config.getAddressForEmitter());
            packet.setData(data);
            logger.debug("Sending UDP packet.");
            daemonSocket.send(packet);
        } catch (Exception e) {
            String segmentName = Optional.ofNullable(entity.getParent()).map(this::nameAndId).orElse("[no parent segment]");
            logger.error("Exception while sending segment over UDP for entity " +  nameAndId(entity) + " on segment "
                         + segmentName, e);
            return false;
        }
        return true;
    }

    private String nameAndId(Entity entity) {
        return entity.getName() + " [" + entity.getId() + "]";
    }
}
