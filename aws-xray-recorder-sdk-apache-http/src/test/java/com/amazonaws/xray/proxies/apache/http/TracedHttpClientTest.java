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

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class TracedHttpClientTest {

    @ClassRule
    public static WireMockClassRule server = new WireMockClassRule(wireMockConfig().dynamicPort());

    private CloseableHttpClient client;

    @Before
    public void setUpClient() {
        client = new TracedHttpClient(HttpClients.createDefault());
    }

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

    @Test
    public void normalPropagation() throws Exception {
        stubFor(any(anyUrl()).willReturn(ok()));

        TraceID traceID = TraceID.fromString("1-67891233-abcdef012345678912345678");
        Segment segment = AWSXRay.beginSegment("test", traceID, null);
        client.execute(new HttpGet(server.baseUrl())).close();
        AWSXRay.endSegment();

        Subsegment subsegment = segment.getSubsegments().get(0);
        verify(getRequestedFor(urlPathEqualTo("/"))
                   .withHeader(TraceHeader.HEADER_KEY,
                               equalTo("Root=1-67891233-abcdef012345678912345678;Parent=" + subsegment.getId() + ";Sampled=1")));
    }

    @Test
    public void unsampledPropagation() throws Exception {
        stubFor(any(anyUrl()).willReturn(ok()));

        AWSXRay.getGlobalRecorder().beginNoOpSegment();
        client.execute(new HttpGet(server.baseUrl())).close();
        AWSXRay.endSegment();

        verify(getRequestedFor(urlPathEqualTo("/"))
                   .withHeader(TraceHeader.HEADER_KEY,
                               equalTo("Root=1-00000000-000000000000000000000000;Sampled=0")));
    }

    @Test
    public void unsampledButForcedPropagation() throws Exception {
        stubFor(any(anyUrl()).willReturn(ok()));

        TraceID traceID = TraceID.fromString("1-67891233-abcdef012345678912345678");
        Segment segment = AWSXRay.beginSegment("test", traceID, null);
        segment.setSampled(false);
        client.execute(new HttpGet(server.baseUrl())).close();
        AWSXRay.endSegment();

        verify(getRequestedFor(urlPathEqualTo("/"))
                   .withHeader(TraceHeader.HEADER_KEY,
                               equalTo("Root=1-67891233-abcdef012345678912345678;Sampled=0")));
    }
}
