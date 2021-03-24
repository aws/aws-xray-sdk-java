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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.amazonaws.xray.AWSXRay;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityTest {

    @BeforeEach
    void setup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testDurationSerialization() {
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        seg.putMetadata("millisecond", Duration.ofMillis(3));
        seg.putMetadata("second", Duration.ofSeconds(1));
        seg.putMetadata("minute", Duration.ofMinutes(55));
        String serializedSeg = seg.serialize();

        String expected = "{\"default\":{\"millisecond\":0.003000000,\"second\":1.000000000,\"minute\":3300.000000000}}";
        assertThat(serializedSeg).contains(expected);
    }

    @Test
    void testTimestampSerialization() {
        Segment seg = new SegmentImpl(AWSXRay.getGlobalRecorder(), "test");
        seg.putMetadata("date", Instant.ofEpochSecond(1616559298));
        String serializedSeg = seg.serialize();

        String expected = "{\"default\":{\"date\":1616559298.000000000}}";
        assertThat(serializedSeg).contains(expected);
    }
}
