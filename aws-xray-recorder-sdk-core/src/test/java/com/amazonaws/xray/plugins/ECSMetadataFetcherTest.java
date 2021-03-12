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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
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

public class ECSMetadataFetcherTest {
    @ClassRule
    public static WireMockClassRule server = new WireMockClassRule(wireMockConfig().dynamicPort());

    private static final String CONTAINER_METADATA =
        "{\n" +
        "   \"DockerId\":\"efgh\",\n" +
        "   \"Name\":\"main\",\n" +
        "   \"DockerName\":\"ecs-helloworld-3-main-f69182c192af9cd71000\",\n" +
        "   \"Image\":\"public.ecr.aws/my-image\",\n" +
        "   \"ImageID\":\"sha256:937f680f89da88d36ddc3cff9ae5a70909fe149ca2d08b99208633730117ad67\",\n" +
        "   \"Labels\":{\n" +
        "      \"com.amazonaws.ecs.cluster\":\"myCluster\"\n" +
        "   },\n" +
        "   \"DesiredStatus\":\"RUNNING\",\n" +
        "   \"KnownStatus\":\"RUNNING\",\n" +
        "   \"Limits\":{\n" +
        "      \"CPU\":256,\n" +
        "      \"Memory\":0\n" +
        "   },\n" +
        "   \"CreatedAt\":\"2020-09-29T19:05:57.744432392Z\",\n" +
        "   \"StartedAt\":\"2020-09-29T19:05:58.656886747Z\",\n" +
        "   \"Type\":\"NORMAL\",\n" +
        "   \"LogDriver\":\"awslogs\",\n" +
        "   \"LogOptions\":{\n" +
        "      \"awslogs-group\":\"my-group\",\n" +
        "      \"awslogs-region\":\"ap-southeast-1\",\n" +
        "      \"awslogs-stream\":\"logs/main/5678\"\n" +
        "   },\n" +
        "   \"ContainerARN\":\"arn:aws:ecs:ap-southeast-1:123456789012:container/567\",\n" +
        "   \"Networks\":[\n" +
        "      {\n" +
        "         \"NetworkMode\":\"host\",\n" +
        "         \"IPv4Addresses\":[\n" +
        "            \"\"\n" +
        "         ]\n" +
        "      }\n" +
        "   ]\n" +
        "}";

    private ECSMetadataFetcher fetcher;

    @Before
    public void setup() {
        fetcher = new ECSMetadataFetcher("http://localhost:" + server.port());
    }

    @Test
    public void testContainerMetadata() {
        stubFor(any(urlPathEqualTo("/")).willReturn(okJson(CONTAINER_METADATA)));

        Map<ECSMetadataFetcher.ECSContainerMetadata, String> metadata = this.fetcher.fetchContainer();

        assertThat(metadata).containsOnly(
            entry(ECSMetadataFetcher.ECSContainerMetadata.CONTAINER_ARN, "arn:aws:ecs:ap-southeast-1:123456789012:container/567"),
            entry(ECSMetadataFetcher.ECSContainerMetadata.LOG_DRIVER, "awslogs"),
            entry(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_REGION, "ap-southeast-1"),
            entry(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_NAME, "my-group")
        );

        verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void testIncompleteResponse() {
        stubFor(any(urlPathEqualTo("/")).willReturn(okJson("{\"DockerId\":\"efgh\"}")));

        Map<ECSMetadataFetcher.ECSContainerMetadata, String> metadata = this.fetcher.fetchContainer();

        assertThat(metadata).isEmpty();
        verify(getRequestedFor(urlEqualTo("/")));
    }

    @Test
    public void testErrorResponse() {
        stubFor(any(urlPathEqualTo("/")).willReturn(okJson("bad json string")));

        Map<ECSMetadataFetcher.ECSContainerMetadata, String> metadata = this.fetcher.fetchContainer();

        assertThat(metadata).isEmpty();
        verify(getRequestedFor(urlEqualTo("/")));
    }
}
