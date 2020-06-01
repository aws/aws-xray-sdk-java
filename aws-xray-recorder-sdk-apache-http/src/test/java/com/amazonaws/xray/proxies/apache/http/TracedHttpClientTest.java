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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpRequest;
import org.junit.Test;

public class TracedHttpClientTest {
    @Test
    public void testGetUrlHttpUriRequest() {
        assertThat(TracedHttpClient.getUrl(new HttpGet("http://amazon.com")), is("http://amazon.com"));
        assertThat(TracedHttpClient.getUrl(new HttpGet("https://docs.aws.amazon.com/xray/latest/devguide/xray-api.html")), is("https://docs.aws.amazon.com/xray/latest/devguide/xray-api.html"));
        assertThat(TracedHttpClient.getUrl(new HttpGet("https://localhost:8443/api/v1")), is("https://localhost:8443/api/v1"));
    }

    @Test
    public void testGetUrlHttpHostHttpRequest() {
        assertThat(TracedHttpClient.getUrl(new HttpHost("amazon.com"), new BasicHttpRequest("GET", "/")), is("http://amazon.com/"));
        assertThat(TracedHttpClient.getUrl(new HttpHost("amazon.com", -1, "https"), new BasicHttpRequest("GET", "/")), is("https://amazon.com/"));
        assertThat(TracedHttpClient.getUrl(new HttpHost("localhost", 8080), new BasicHttpRequest("GET", "/api/v1")), is("http://localhost:8080/api/v1"));
        assertThat(TracedHttpClient.getUrl(new HttpHost("localhost", 8443, "https"), new BasicHttpRequest("GET", "/api/v1")), is("https://localhost:8443/api/v1"));

        assertThat(TracedHttpClient.getUrl(new HttpHost("amazon.com", -1, "https"), new BasicHttpRequest("GET", "https://docs.aws.amazon.com/xray/latest/devguide/xray-api.html")), is("https://docs.aws.amazon.com/xray/latest/devguide/xray-api.html"));
    }
}
