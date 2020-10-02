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

package com.amazonaws.xray.entities;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class TraceIDTest {

    @BeforeAll
    static void beforeAll() {
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.defaultRecorder());
    }

    // Chance this test passes once even when broken but inconceivable to pass several times.
    @RepeatedTest(10)
    void create() {
        int startTimeSecs = (int) Instant.now().getEpochSecond();
        TraceID traceID = TraceID.create();
        assertThat(Integer.parseInt(traceID.getStartTimeAsHex(), 16)).isGreaterThanOrEqualTo(startTimeSecs);
        assertThat(Integer.parseInt(traceID.getStartTimeAsHex(), 16)).isEqualTo(traceID.getStartTime());
        assertThat(traceID.getNumberAsHex()).hasSize(24).satisfies(TraceID::isHex);
        assertThat(traceID.getNumberAsHex()).isEqualTo(padLeft(traceID.getNumber().toString(16), 24));
    }

    @Test
    void fromString() {
        TraceID traceID = TraceID.fromString("1-57ff426a-80c11c39b0c928905eb0828d");
        assertThat(traceID.getStartTimeAsHex()).isEqualTo("57ff426a");
        assertThat(traceID.getNumberAsHex()).isEqualTo("80c11c39b0c928905eb0828d");
    }

    @Test
    void fromString_invalidLength() {
        TraceID traceID = TraceID.fromString("1-57ff426a-80c11c39b0c928905eb0828d1");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    @Test
    void fromString_invalidVersion() {
        TraceID traceID = TraceID.fromString("2-57ff426a-80c11c39b0c928905eb0828d");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    @Test
    void fromString_invalidDelimiter1() {
        TraceID traceID = TraceID.fromString("2+57ff426a-80c11c39b0c928905eb0828d");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    @Test
    void fromString_invalidDelimiter2() {
        TraceID traceID = TraceID.fromString("2+57ff426a+80c11c39b0c928905eb0828d");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    @Test
    void fromString_invalidStartTime() {
        TraceID traceID = TraceID.fromString("2+57fg426a+80c11c39b0c928905eb0828d");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    @Test
    void fromString_invalidNumber() {
        TraceID traceID = TraceID.fromString("2+57ff426a+80c11c39b0c928905gb0828d");
        // Invalid means new trace ID so epoch will not match.
        assertThat(traceID.getStartTimeAsHex()).isNotEqualTo("57ff426a");
    }

    private static String padLeft(String str, int size) {
        if (str.length() == size) {
            return str;
        }
        StringBuilder padded = new StringBuilder(size);
        for (int i = str.length(); i < size; i++) {
            padded.append('0');
        }
        padded.append(str);
        return padded.toString();
    }
}
