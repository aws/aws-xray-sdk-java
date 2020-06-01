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

package com.amazonaws.xray.proxies.apache.http;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.emitters.Emitter;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@FixMethodOrder(MethodSorters.JVM)
public class TracedResponseHandlerTest {

    @Before
    public void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testHandleResponse200SetsNoFlags() {
        Segment segment = segmentInResponseToCode(200);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assert.assertFalse(subsegment.isFault());
        Assert.assertFalse(subsegment.isError());
        Assert.assertFalse(subsegment.isThrottle());
    }

    @Test
    public void testHandleResponse400SetsErrorFlag() {
        Segment segment = segmentInResponseToCode(400);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assert.assertFalse(subsegment.isFault());
        Assert.assertTrue(subsegment.isError());
        Assert.assertFalse(subsegment.isThrottle());
    }

    @Test
    public void testHandleResponse429SetsErrorAndThrottleFlag() {
        Segment segment = segmentInResponseToCode(429);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assert.assertFalse(subsegment.isFault());
        Assert.assertTrue(subsegment.isError());
        Assert.assertTrue(subsegment.isThrottle());
    }

    @Test
    public void testHandleResponse500SetsFaultFlag() {
        Segment segment = segmentInResponseToCode(500);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assert.assertTrue(subsegment.isFault());
        Assert.assertFalse(subsegment.isError());
        Assert.assertFalse(subsegment.isThrottle());
    }

    private class NoOpResponseHandler implements ResponseHandler<String> {
        public String handleResponse(HttpResponse response) {
            return "no-op";
        }
    }

    private Segment segmentInResponseToCode(int code) {
        NoOpResponseHandler responseHandler = new NoOpResponseHandler();
        TracedResponseHandler<String> tracedResponseHandler = new TracedResponseHandler<>(responseHandler);
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, code, ""));

        Segment segment = AWSXRay.beginSegment("test");
        AWSXRay.beginSubsegment("someHttpCall");

        try {
            tracedResponseHandler.handleResponse(httpResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AWSXRay.endSubsegment();
        AWSXRay.endSegment();
        return segment;
    }
}
