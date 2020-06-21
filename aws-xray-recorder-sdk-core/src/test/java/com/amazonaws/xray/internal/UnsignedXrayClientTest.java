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

package com.amazonaws.xray.internal;

import static com.amazonaws.xray.internal.UnsignedXrayClient.OBJECT_MAPPER;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.xray.model.GetSamplingRulesRequest;
import com.amazonaws.services.xray.model.GetSamplingRulesResult;
import com.amazonaws.services.xray.model.GetSamplingTargetsRequest;
import com.amazonaws.services.xray.model.GetSamplingTargetsResult;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class UnsignedXrayClientTest {

    @ClassRule
    public static WireMockClassRule server = new WireMockClassRule(wireMockConfig().dynamicPort());

    private static final String SAMPLING_RULES =
        "{\n"
        + "    \"SamplingRuleRecords\": [\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"Default\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/Default\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 10000,\n"
        + "                \"FixedRate\": 0.01,\n"
        + "                \"ReservoirSize\": 0,\n"
        + "                \"ServiceName\": \"*\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"*\",\n"
        + "                \"URLPath\": \"*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 0.0,\n"
        + "            \"ModifiedAt\": 1530558121.0\n"
        + "        },\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"base-scorekeep\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/base-scorekeep\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 9000,\n"
        + "                \"FixedRate\": 0.1,\n"
        + "                \"ReservoirSize\": 2,\n"
        + "                \"ServiceName\": \"Scorekeep\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"*\",\n"
        + "                \"URLPath\": \"*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 1530573954.0,\n"
        + "            \"ModifiedAt\": 1530920505.0\n"
        + "        },\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"polling-scorekeep\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/polling-scorekeep\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 5000,\n"
        + "                \"FixedRate\": 0.003,\n"
        + "                \"ReservoirSize\": 0,\n"
        + "                \"ServiceName\": \"Scorekeep\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"GET\",\n"
        + "                \"URLPath\": \"/api/state/*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 1530918163.0,\n"
        + "            \"ModifiedAt\": 1530918163.0\n"
        + "        }\n"
        + "    ]\n"
        + "}{\n"
        + "    \"SamplingRuleRecords\": [\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"Default\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/Default\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 10000,\n"
        + "                \"FixedRate\": 0.01,\n"
        + "                \"ReservoirSize\": 0,\n"
        + "                \"ServiceName\": \"*\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"*\",\n"
        + "                \"URLPath\": \"*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 0.0,\n"
        + "            \"ModifiedAt\": 1530558121.0\n"
        + "        },\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"base-scorekeep\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/base-scorekeep\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 9000,\n"
        + "                \"FixedRate\": 0.1,\n"
        + "                \"ReservoirSize\": 2,\n"
        + "                \"ServiceName\": \"Scorekeep\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"*\",\n"
        + "                \"URLPath\": \"*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 1530573954.0,\n"
        + "            \"ModifiedAt\": 1530920505.0\n"
        + "        },\n"
        + "        {\n"
        + "            \"SamplingRule\": {\n"
        + "                \"RuleName\": \"polling-scorekeep\",\n"
        + "                \"RuleARN\": \"arn:aws:xray:us-east-1::sampling-rule/polling-scorekeep\",\n"
        + "                \"ResourceARN\": \"*\",\n"
        + "                \"Priority\": 5000,\n"
        + "                \"FixedRate\": 0.003,\n"
        + "                \"ReservoirSize\": 0,\n"
        + "                \"ServiceName\": \"Scorekeep\",\n"
        + "                \"ServiceType\": \"*\",\n"
        + "                \"Host\": \"*\",\n"
        + "                \"HTTPMethod\": \"GET\",\n"
        + "                \"URLPath\": \"/api/state/*\",\n"
        + "                \"Version\": 1,\n"
        + "                \"Attributes\": {}\n"
        + "            },\n"
        + "            \"CreatedAt\": 1530918163.0,\n"
        + "            \"ModifiedAt\": 1530918163.0\n"
        + "        }\n"
        + "    ]\n"
        + "}";

    private static final String SAMPLING_TARGETS =
        "{\n"
        + "    \"SamplingTargetDocuments\": [\n"
        + "        {\n"
        + "            \"RuleName\": \"base-scorekeep\",\n"
        + "            \"FixedRate\": 0.1,\n"
        + "            \"ReservoirQuota\": 2,\n"
        + "            \"ReservoirQuotaTTL\": 1530923107.0,\n"
        + "            \"Interval\": 10\n"
        + "        },\n"
        + "        {\n"
        + "            \"RuleName\": \"polling-scorekeep\",\n"
        + "            \"FixedRate\": 0.003,\n"
        + "            \"ReservoirQuota\": 0,\n"
        + "            \"ReservoirQuotaTTL\": 1530923107.0,\n"
        + "            \"Interval\": 10\n"
        + "        }\n"
        + "    ],\n"
        + "    \"LastRuleModification\": 1530920505.0,\n"
        + "    \"UnprocessedStatistics\": []\n"
        + "}{\n"
        + "    \"SamplingTargetDocuments\": [\n"
        + "        {\n"
        + "            \"RuleName\": \"base-scorekeep\",\n"
        + "            \"FixedRate\": 0.1,\n"
        + "            \"ReservoirQuota\": 2,\n"
        + "            \"ReservoirQuotaTTL\": 1530923107.0,\n"
        + "            \"Interval\": 10\n"
        + "        },\n"
        + "        {\n"
        + "            \"RuleName\": \"polling-scorekeep\",\n"
        + "            \"FixedRate\": 0.003,\n"
        + "            \"ReservoirQuota\": 0,\n"
        + "            \"ReservoirQuotaTTL\": 1530923107.0,\n"
        + "            \"Interval\": 10\n"
        + "        }\n"
        + "    ],\n"
        + "    \"LastRuleModification\": 1530920505.0,\n"
        + "    \"UnprocessedStatistics\": []\n"
        + "}";

    private UnsignedXrayClient client;

    @Before
    public void setUp() {
        client = new UnsignedXrayClient(server.baseUrl());
    }

    @Test
    public void getSamplingRules() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody(SAMPLING_RULES)));

        GetSamplingRulesResult result = client.getSamplingRules(new GetSamplingRulesRequest());

        GetSamplingRulesResult expected = OBJECT_MAPPER.readValue(SAMPLING_RULES, GetSamplingRulesResult.class);
        assertThat(expected).isEqualTo(result);

        verify(postRequestedFor(urlEqualTo("/GetSamplingRules"))
                       .withHeader("Content-Type", equalTo("application/json"))
                       .withRequestBody(equalToJson("{}")));
    }

    @Test
    public void getSamplingTargets() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody(SAMPLING_TARGETS)));

        GetSamplingTargetsRequest request = new GetSamplingTargetsRequest()
            .withSamplingStatisticsDocuments(new SamplingStatisticsDocument().withClientID("client-id"));

        GetSamplingTargetsResult result = client.getSamplingTargets(request);

        GetSamplingTargetsResult expected = OBJECT_MAPPER.readValue(SAMPLING_TARGETS, GetSamplingTargetsResult.class);
        assertThat(expected).isEqualTo(result);

        verify(postRequestedFor(urlEqualTo("/SamplingTargets"))
                       .withHeader("Content-Type", equalTo("application/json"))
                       .withRequestBody(equalToJson("{"
                                                    + " \"SamplingStatisticsDocuments\": ["
                                                    + "    {"
                                                    + "      \"ClientID\": \"client-id\""
                                                    + "    }"
                                                    + " ] "
                                                    + "}")));
    }

    @Test
    public void badStatus() {
        String expectedMessage = "{\"message\": \"bad authentication\"}";

        stubFor(any(anyUrl()).willReturn(aResponse()
                                                 .withStatus(500)
                                                 .withBody(expectedMessage)));

        assertThatThrownBy(() -> client.getSamplingRules(new GetSamplingRulesRequest()))
                .isInstanceOf(XrayClientException.class)
                .hasMessageContaining(expectedMessage);
    }

    // This test may be flaky, it's not testing much so delete if it ever flakes.
    @Test
    public void cannotSend() {
        client = new UnsignedXrayClient("http://localhost:" + (server.port() + 1234));

        assertThatThrownBy(() -> client.getSamplingRules(new GetSamplingRulesRequest()))
                .isInstanceOf(XrayClientException.class)
                .hasMessageContaining("Could not serialize and send request");
    }
}
