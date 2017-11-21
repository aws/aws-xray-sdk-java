package com.amazonaws.xray.emitters;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;

public abstract class Emitter {
    protected static final String PROTOCOL_HEADER = "{\"format\": \"json\", \"version\": 1}";
    protected static final String PRIORITY_PROTOCOL_HEADER = "{\"format\": \"json\", \"version\": 1}";
    protected static final char PROTOCOL_DELIMITER = '\n';
    protected static final int DAEMON_BUF_RECEIVE_SIZE = 256 * 1024; // daemon.go#line-15

    /**
     * Sends a segment to the X-Ray daemon.
     *
     * @param segment
     *            the segment to send
     * @return true if the send operation was successful
     */
    public abstract boolean sendSegment(Segment segment);

    /**
     * Sends a subsegment to the X-Ray daemon.
     *
     * @param subsegment
     *  the subsegment to send
     * @return
     *  true if the send operation was successful
     *
     */
    public abstract boolean sendSubsegment(Subsegment subsegment);
}
