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

package com.amazonaws.xray;

import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceHeader.SampleDecision;
import com.amazonaws.xray.entities.TraceID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TraceHeaderTest {

    private static final String TRACE_ID = "1-57ff426a-80c11c39b0c928905eb0828d";

    @Test
    void testSampledEqualsOneFromString() {
        TraceHeader header = TraceHeader.fromString("Sampled=1");
        Assertions.assertEquals(SampleDecision.SAMPLED, header.getSampled());
        Assertions.assertNull(header.getRootTraceId());
        Assertions.assertNull(header.getParentId());
        Assertions.assertTrue(header.getAdditionalParams().isEmpty());
    }

    @Test
    void testLongHeaderFromString() {
        TraceHeader header = TraceHeader.fromString("Sampled=?;Root=" + TRACE_ID + ";Parent=foo;Self=2;Foo=bar");
        Assertions.assertEquals(SampleDecision.REQUESTED, header.getSampled());
        Assertions.assertEquals(TraceID.fromString(TRACE_ID), header.getRootTraceId());
        Assertions.assertEquals("foo", header.getParentId());
        Assertions.assertEquals(1, header.getAdditionalParams().size());
        Assertions.assertEquals("bar", header.getAdditionalParams().get("Foo"));
    }

    @Test
    void testLongHeaderFromStringWithSpaces() {
        TraceHeader header = TraceHeader.fromString("Sampled=?; Root=" + TRACE_ID + "; Parent=foo; Self=2; Foo=bar");
        Assertions.assertEquals(SampleDecision.REQUESTED, header.getSampled());
        Assertions.assertEquals(TraceID.fromString(TRACE_ID), header.getRootTraceId());
        Assertions.assertEquals("foo", header.getParentId());
        Assertions.assertEquals(1, header.getAdditionalParams().size());
        Assertions.assertEquals("bar", header.getAdditionalParams().get("Foo"));
    }

    @Test
    void testSampledUnknownToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.UNKNOWN);
        Assertions.assertEquals("", header.toString());
    }

    @Test
    void testSampledEqualsOneWithSamplingRequestedToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.REQUESTED);
        header.setSampled(SampleDecision.SAMPLED);
        Assertions.assertEquals("Sampled=1", header.toString());
    }

    @Test
    void testSampledEqualsOneAndParentToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.SAMPLED);
        header.setParentId("foo");
        Assertions.assertEquals("Parent=foo;Sampled=1", header.toString());
    }

    @Test
    void testLongHeaderToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.SAMPLED);
        header.setRootTraceId(TraceID.fromString(TRACE_ID));
        header.setParentId("foo");
        header.getAdditionalParams().put("Foo", "bar");
        Assertions.assertEquals("Root=" + TRACE_ID + ";Parent=foo;Sampled=1;Foo=bar", header.toString());
    }
}
