package com.amazonaws.xray.metrics;

import com.amazonaws.xray.config.MetricsDaemonConfiguration;
import com.amazonaws.xray.entities.Segment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Generates EMF formatted metrics and send them to the CloudWatch Agent for publication.
 */
public class UDPMetricEmitter implements MetricEmitter {
    private static final Log logger = LogFactory.getLog(UDPMetricEmitter.class);
    private static final int BUFFER_SIZE = 64 * 1024;

    private MetricFormatter formatter;
    private DatagramSocket socket;
    private InetSocketAddress address;

    private byte[] sendBuffer = new byte[BUFFER_SIZE];

    public UDPMetricEmitter() throws SocketException {
        MetricsDaemonConfiguration configuration = new MetricsDaemonConfiguration();

        formatter = new EMFMetricFormatter();
        try {
            socket = new DatagramSocket();
            address = configuration.getAddressForEmitter();
        } catch (SocketException e) {
            logger.error("Exception while instantiating daemon socket.", e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitMetric(final Segment segment) {
        String formattedMetric = formatter.formatSegment(segment);
        DatagramPacket packet = new DatagramPacket(sendBuffer, BUFFER_SIZE, address);
        packet.setData(formattedMetric.getBytes());
        try {
            socket.send(packet);
        } catch (IOException e) {
            logger.error("Unable to send metric to agent.", e);
        }
    }
}
