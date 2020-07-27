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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TracedResponseHandlerTest {

    @BeforeEach
    void setupAWSXRay() {
        Emitter blankEmitter = Mockito.mock(Emitter.class);
        Mockito.doReturn(true).when(blankEmitter).sendSegment(Mockito.anyObject());
        Mockito.doReturn(true).when(blankEmitter).sendSubsegment(Mockito.anyObject());
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.standard().withEmitter(blankEmitter).build());
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testHandleResponse200SetsNoFlags() {
        Segment segment = segmentInResponseToCode(200);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assertions.assertFalse(subsegment.isFault());
        Assertions.assertFalse(subsegment.isError());
        Assertions.assertFalse(subsegment.isThrottle());
    }

    @Test
    void testHandleResponse400SetsErrorFlag() {
        Segment segment = segmentInResponseToCode(400);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assertions.assertFalse(subsegment.isFault());
        Assertions.assertTrue(subsegment.isError());
        Assertions.assertFalse(subsegment.isThrottle());
    }

    @Test
    void testHandleResponse429SetsErrorAndThrottleFlag() {
        Segment segment = segmentInResponseToCode(429);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assertions.assertFalse(subsegment.isFault());
        Assertions.assertTrue(subsegment.isError());
        Assertions.assertTrue(subsegment.isThrottle());
    }

    @Test
    void testHandleResponse500SetsFaultFlag() {
        Segment segment = segmentInResponseToCode(500);
        Subsegment subsegment = segment.getSubsegments().get(0);
        Assertions.assertTrue(subsegment.isFault());
        Assertions.assertFalse(subsegment.isError());
        Assertions.assertFalse(subsegment.isThrottle());
    }

    private static class NoOpResponseHandler implements ResponseHandler<String> {
        @Override
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
