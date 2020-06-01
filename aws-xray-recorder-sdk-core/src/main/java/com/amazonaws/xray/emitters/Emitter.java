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
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import java.io.IOException;

/**
 * An emitter of segments and subsegments to X-Ray.
 */
public abstract class Emitter {

    protected static final String PROTOCOL_HEADER = "{\"format\": \"json\", \"version\": 1}";
    protected static final String PRIORITY_PROTOCOL_HEADER = "{\"format\": \"json\", \"version\": 1}";
    protected static final char PROTOCOL_DELIMITER = '\n';
    protected static final int DAEMON_BUF_RECEIVE_SIZE = 256 * 1024; // daemon.go#line-15

    /**
     * Returns an {@link Emitter} that uses a default {@link DaemonConfiguration}.
     *
     * @throws IOException if an error occurs while instantiating the {@link Emitter} (e.g., socket failure).
     */
    public static Emitter create() throws IOException {
        return new UDPEmitter();
    }

    /**
     * Returns an {@link Emitter} that uses the provided {@link DaemonConfiguration}.
     *
     * @throws IOException if an error occurs while instantiating the {@link Emitter} (e.g., socket failure).
     */
    public static Emitter create(DaemonConfiguration configuration) throws IOException {
        return new UDPEmitter(configuration);
    }

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
