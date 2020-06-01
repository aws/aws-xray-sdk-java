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
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class TraceHeaderTest {

    private static final String TRACE_ID = "1-57ff426a-80c11c39b0c928905eb0828d";

    @Test
    public void testSampledEqualsOneFromString() {
        TraceHeader header = TraceHeader.fromString("Sampled=1");
        Assert.assertEquals(SampleDecision.SAMPLED, header.getSampled());
        Assert.assertNull(header.getRootTraceId());
        Assert.assertNull(header.getParentId());
        Assert.assertTrue(header.getAdditionalParams().isEmpty());
    }

    @Test
    public void testLongHeaderFromString() {
        TraceHeader header = TraceHeader.fromString("Sampled=?;Root=" + TRACE_ID + ";Parent=foo;Self=2;Foo=bar");
        Assert.assertEquals(SampleDecision.REQUESTED, header.getSampled());
        Assert.assertEquals(TraceID.fromString(TRACE_ID), header.getRootTraceId());
        Assert.assertEquals("foo", header.getParentId());
        Assert.assertEquals(1, header.getAdditionalParams().size());
        Assert.assertEquals("bar", header.getAdditionalParams().get("Foo"));
    }

    @Test
    public void testLongHeaderFromStringWithSpaces() {
        TraceHeader header = TraceHeader.fromString("Sampled=?; Root=" + TRACE_ID + "; Parent=foo; Self=2; Foo=bar");
        Assert.assertEquals(SampleDecision.REQUESTED, header.getSampled());
        Assert.assertEquals(TraceID.fromString(TRACE_ID), header.getRootTraceId());
        Assert.assertEquals("foo", header.getParentId());
        Assert.assertEquals(1, header.getAdditionalParams().size());
        Assert.assertEquals("bar", header.getAdditionalParams().get("Foo"));
    }

    @Test
    public void testSampledUnknownToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.UNKNOWN);
        Assert.assertEquals("", header.toString());
    }

    @Test
    public void testSampledEqualsOneWithSamplingRequestedToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.REQUESTED);
        header.setSampled(SampleDecision.SAMPLED);
        Assert.assertEquals("Sampled=1", header.toString());
    }

    @Test
    public void testSampledEqualsOneAndParentToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.SAMPLED);
        header.setParentId("foo");
        Assert.assertEquals("Parent=foo;Sampled=1", header.toString());
    }

    @Test
    public void testLongHeaderToString() {
        TraceHeader header = new TraceHeader();
        header.setSampled(SampleDecision.SAMPLED);
        header.setRootTraceId(TraceID.fromString(TRACE_ID));
        header.setParentId("foo");
        header.getAdditionalParams().put("Foo", "bar");
        Assert.assertEquals("Root=" + TRACE_ID + ";Parent=foo;Sampled=1;Foo=bar", header.toString());
    }
}
