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

import static com.amazonaws.xray.AWSXRay.getGlobalRecorder;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.entities.DummySegment;
import java.net.SocketException;
import org.junit.jupiter.api.Test;

class UDPEmitterTest {

    @Test
    void testCustomAddress() throws SocketException {
        String address = "123.4.5.6:1234";
        DaemonConfiguration config = getDaemonConfiguration(address);

        UDPEmitter emitter = new UDPEmitter(config);

        assertThat(emitter.getUDPAddress()).isEqualTo(address);
    }


    @Test
    void sendingSegmentShouldNotThrowExceptions() throws SocketException {
        DaemonConfiguration config = getDaemonConfiguration("__udpemittertest_unresolvable__:1234");
        UDPEmitter emitter = new UDPEmitter(config);

        boolean success = emitter.sendSegment(new DummySegment(getGlobalRecorder()));
        assertThat(success).isFalse();
    }

    protected DaemonConfiguration getDaemonConfiguration(final String address) {
        DaemonConfiguration config = new DaemonConfiguration();
        config.setDaemonAddress(address);
        return config;
    }
}
