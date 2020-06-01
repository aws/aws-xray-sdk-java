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

package com.amazonaws.xray.plugins;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class EC2MetadataFetcherTest {

    @ClassRule
    public static WireMockClassRule server = new WireMockClassRule(wireMockConfig().dynamicPort());

    // From https://docs.amazonaws.cn/en_us/AWSEC2/latest/UserGuide/instance-identity-documents.html
    private static final String IDENTITY_DOCUMENT =
        "{\n"
        + "    \"devpayProductCodes\" : null,\n"
        + "    \"marketplaceProductCodes\" : [ \"1abc2defghijklm3nopqrs4tu\" ], \n"
        + "    \"availabilityZone\" : \"us-west-2b\",\n"
        + "    \"privateIp\" : \"10.158.112.84\",\n"
        + "    \"version\" : \"2017-09-30\",\n"
        + "    \"instanceId\" : \"i-1234567890abcdef0\",\n"
        + "    \"billingProducts\" : null,\n"
        + "    \"instanceType\" : \"t2.micro\",\n"
        + "    \"accountId\" : \"123456789012\",\n"
        + "    \"imageId\" : \"ami-5fb8c835\",\n"
        + "    \"pendingTime\" : \"2016-11-19T16:32:11Z\",\n"
        + "    \"architecture\" : \"x86_64\",\n"
        + "    \"kernelId\" : null,\n"
        + "    \"ramdiskId\" : null,\n"
        + "    \"region\" : \"us-west-2\"\n"
        + "}";

    private EC2MetadataFetcher fetcher;

    @Before
    public void setUp() {
        fetcher = new EC2MetadataFetcher("localhost:" + server.port());
    }

    @Test
    public void imdsv2() {
        stubFor(any(urlPathEqualTo("/latest/api/token")).willReturn(ok("token")));
        stubFor(any(urlPathEqualTo("/latest/dynamic/instance-identity/document"))
                    .willReturn(okJson(IDENTITY_DOCUMENT)));

        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = fetcher.fetch();
        assertThat(metadata).containsOnly(
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_ID, "i-1234567890abcdef0"),
            entry(EC2MetadataFetcher.EC2Metadata.AVAILABILITY_ZONE, "us-west-2b"),
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_TYPE, "t2.micro"),
            entry(EC2MetadataFetcher.EC2Metadata.AMI_ID, "ami-5fb8c835"));

        verify(putRequestedFor(urlEqualTo("/latest/api/token"))
                   .withHeader("X-aws-ec2-metadata-token-ttl-seconds", equalTo("60")));
        verify(getRequestedFor(urlEqualTo("/latest/dynamic/instance-identity/document"))
                   .withHeader("X-aws-ec2-metadata-token", equalTo("token")));
    }

    @Test
    public void imdsv1() {
        stubFor(any(urlPathEqualTo("/latest/api/token")).willReturn(notFound()));
        stubFor(any(urlPathEqualTo("/latest/dynamic/instance-identity/document"))
                    .willReturn(okJson(IDENTITY_DOCUMENT)));

        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = fetcher.fetch();
        assertThat(metadata).containsOnly(
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_ID, "i-1234567890abcdef0"),
            entry(EC2MetadataFetcher.EC2Metadata.AVAILABILITY_ZONE, "us-west-2b"),
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_TYPE, "t2.micro"),
            entry(EC2MetadataFetcher.EC2Metadata.AMI_ID, "ami-5fb8c835"));

        verify(putRequestedFor(urlEqualTo("/latest/api/token"))
                   .withHeader("X-aws-ec2-metadata-token-ttl-seconds", equalTo("60")));
        verify(getRequestedFor(urlEqualTo("/latest/dynamic/instance-identity/document"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
    }

    @Test
    public void badJson() {
        stubFor(any(urlPathEqualTo("/latest/api/token")).willReturn(notFound()));
        stubFor(any(urlPathEqualTo("/latest/dynamic/instance-identity/document"))
                    .willReturn(okJson("I'm not JSON")));

        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = fetcher.fetch();
        assertThat(metadata).isEmpty();

        verify(putRequestedFor(urlEqualTo("/latest/api/token"))
                   .withHeader("X-aws-ec2-metadata-token-ttl-seconds", equalTo("60")));
        verify(getRequestedFor(urlEqualTo("/latest/dynamic/instance-identity/document"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
    }
}
