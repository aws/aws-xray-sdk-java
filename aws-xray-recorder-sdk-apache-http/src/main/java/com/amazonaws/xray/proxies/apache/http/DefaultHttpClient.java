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
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Subsegment;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

/**
 * @deprecated Apache 4.3
 *
 * Wraps and overrides {@code org.apache.http.impl.client.DefaultHttpClient}'s execute() methods. Accesses the global recorder
 * upon each invocation to generate {@code Segment}s.
 *
 * Only overrides those signatures which directly invoke doExecute. Other execute() signatures are wrappers which call these
 * overridden methods.
 *
 */
@Deprecated
public class DefaultHttpClient extends org.apache.http.impl.client.DefaultHttpClient {

    private AWSXRayRecorder getRecorder() {
        return AWSXRay.getGlobalRecorder();
    }

    @Override
    public CloseableHttpResponse execute(
        HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(target.getHostName());
        try {
            TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
            CloseableHttpResponse response = super.execute(target, request, context);
            TracedResponseHandler.addResponseInformation(subsegment, response);
            return response;
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            getRecorder().endSubsegment();
        }
    }

    @Override
    public CloseableHttpResponse execute(
        HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(TracedHttpClient.determineTarget(request).getHostName());
        try {
            TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(request));
            CloseableHttpResponse response = super.execute(request, context);
            TracedResponseHandler.addResponseInformation(subsegment, response);
            return response;
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            getRecorder().endSubsegment();
        }
    }

    @Override
    public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        Subsegment subsegment = getRecorder().beginSubsegment(target.getHostName());
        try {
            TracedHttpClient.addRequestInformation(subsegment, request, TracedHttpClient.getUrl(target, request));
            CloseableHttpResponse response = super.execute(target, request);
            TracedResponseHandler.addResponseInformation(subsegment, response);
            return response;
        } catch (Exception e) {
            subsegment.addException(e);
            throw e;
        } finally {
            getRecorder().endSubsegment();
        }
    }

}
