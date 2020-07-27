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

package com.amazonaws.xray.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EMFMetricFormatterTest {
    private static final String TEST_SERVICE = "testService";
    private static final String TEST_ORIGIN = "Test::Origin";
    private static final double START_TIME = 550378800.0d;
    private static final double END_TIME = 550382400.0d;

    private static final String EXPECTED_JSON =
        "{\"Timestamp\":550382400000,\"log_group_name\":\"ServiceMetricsSDK\",\"CloudWatchMetrics\":"
        + "[{\"Metrics\":[{\"Name\":\"ErrorRate\",\"Unit\":\"None\"},{\"Name\":\"FaultRate\","
        + "\"Unit\":\"None\"},{\"Name\":\"ThrottleRate\",\"Unit\":\"None\"},{\"Name\":\"OkRate\","
        + "\"Unit\":\"None\"},{\"Name\":\"Latency\",\"Unit\":\"Milliseconds\"}],\"Namespace\":"
        + "\"ServiceMetrics/SDK\",\"Dimensions\":[[\"ServiceType\",\"ServiceName\"]]}],\"Latency"
        + "\":3600000.000,\"ErrorRate\":1,\"FaultRate\":1,\"ThrottleRate\":1,\"OkRate\":0,"
        + "\"TraceId\":\"1-5759e988-bd862e3fe1be46a994272793\",\"ServiceType\":\"Test::Origin\","
        + "\"ServiceName\":\"testService\",\"Version\":\"0\"}";


    private Segment testSegment;
    private EMFMetricFormatter formatter;

    @BeforeEach
    void setup() {


        TraceID traceId = TraceID.fromString("1-5759e988-bd862e3fe1be46a994272793");

        testSegment = mock(Segment.class);
        when(testSegment.getTraceId()).thenReturn(traceId);
        when(testSegment.getStartTime()).thenReturn(START_TIME);
        when(testSegment.getEndTime()).thenReturn(END_TIME);
        when(testSegment.isError()).thenReturn(true);
        when(testSegment.isFault()).thenReturn(true);
        when(testSegment.isThrottle()).thenReturn(true);
        when(testSegment.getName()).thenReturn(TEST_SERVICE);
        when(testSegment.getOrigin()).thenReturn(TEST_ORIGIN);

        formatter = new EMFMetricFormatter();
    }


    @Test
    void testJsonFormat() {
        String json = formatter.formatSegment(testSegment);
        Assertions.assertEquals(EXPECTED_JSON, json);
    }

    @Test
    void jsonContainsNoNewlines() {
        String json = formatter.formatSegment(testSegment);
        Assertions.assertFalse(json.contains("\n"));
    }
}
