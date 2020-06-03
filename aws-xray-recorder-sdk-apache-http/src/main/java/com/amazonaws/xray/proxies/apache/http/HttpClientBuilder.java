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
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Wraps and overrides some of {@code org.apache.http.impl.client.HttpClientBuilder}'s methods. Will build a TracedHttpClient
 * wrapping the usual CloseableHttpClient result. Uses the global recorder by default, with an option to provide an alternative.
 */
public class HttpClientBuilder extends org.apache.http.impl.client.HttpClientBuilder {

    private AWSXRayRecorder recorder;

    protected HttpClientBuilder() {
        super();
    }

    public static HttpClientBuilder create() {
        HttpClientBuilder newBuilder = new HttpClientBuilder();
        newBuilder.setRecorder(AWSXRay.getGlobalRecorder());
        return newBuilder;
    }

    public HttpClientBuilder setRecorder(AWSXRayRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    @Override
    public CloseableHttpClient build() {
        CloseableHttpClient result = super.build();
        return new TracedHttpClient(result, recorder);
    }
}
