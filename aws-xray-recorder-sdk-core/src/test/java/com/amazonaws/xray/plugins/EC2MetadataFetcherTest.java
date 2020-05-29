package com.amazonaws.xray.plugins;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
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

    private EC2MetadataFetcher fetcher;

    @Before
    public void setUp() {
        fetcher = new EC2MetadataFetcher("localhost:" + server.port());
    }

    @Test
    public void idmsv2() {
        stubFor(any(urlPathEqualTo("/latest/api/token")).willReturn(ok("token")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/instance-id")).willReturn(ok("instance-123")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/placement/availability-zone")).willReturn(ok("asia-northeast-1a")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/instance-type")).willReturn(ok("m4.xlarge")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/ami-id")).willReturn(ok("ami-1234")));

        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = fetcher.fetch();
        assertThat(metadata).containsExactly(
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_ID, "instance-123"),
            entry(EC2MetadataFetcher.EC2Metadata.AVAILABILITY_ZONE, "asia-northeast-1a"),
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_TYPE, "m4.xlarge"),
            entry(EC2MetadataFetcher.EC2Metadata.AMI_ID, "ami-1234"));

        verify(putRequestedFor(urlEqualTo("/latest/api/token"))
                   .withHeader("X-aws-ec2-metadata-token-ttl-seconds", equalTo("60")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-id"))
                   .withHeader("X-aws-ec2-metadata-token", equalTo("token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/availability-zone"))
                   .withHeader("X-aws-ec2-metadata-token", equalTo("token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-type"))
                   .withHeader("X-aws-ec2-metadata-token", equalTo("token")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/ami-id"))
                   .withHeader("X-aws-ec2-metadata-token", equalTo("token")));
    }

    @Test
    public void idmsv1() {
        stubFor(any(urlPathEqualTo("/latest/api/token")).willReturn(notFound()));
        stubFor(any(urlPathEqualTo("/latest/meta-data/instance-id")).willReturn(ok("instance-123")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/placement/availability-zone")).willReturn(ok("asia-northeast-1a")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/instance-type")).willReturn(ok("m4.xlarge")));
        stubFor(any(urlPathEqualTo("/latest/meta-data/ami-id")).willReturn(ok("ami-1234")));

        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = fetcher.fetch();
        assertThat(metadata).containsExactly(
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_ID, "instance-123"),
            entry(EC2MetadataFetcher.EC2Metadata.AVAILABILITY_ZONE, "asia-northeast-1a"),
            entry(EC2MetadataFetcher.EC2Metadata.INSTANCE_TYPE, "m4.xlarge"),
            entry(EC2MetadataFetcher.EC2Metadata.AMI_ID, "ami-1234"));

        verify(putRequestedFor(urlEqualTo("/latest/api/token"))
                   .withHeader("X-aws-ec2-metadata-token-ttl-seconds", equalTo("60")));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-id"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/placement/availability-zone"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/instance-type"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
        verify(getRequestedFor(urlEqualTo("/latest/meta-data/ami-id"))
                   .withoutHeader("X-aws-ec2-metadata-token"));
    }
}
