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

import java.util.Date;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.amazonaws.services.xray.model.GetSamplingRulesRequest;
import com.amazonaws.services.xray.model.GetSamplingRulesResult;
import com.amazonaws.services.xray.model.GetSamplingTargetsRequest;
import com.amazonaws.services.xray.model.GetSamplingTargetsResult;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

public class UnsignedXrayClientTest {

    @ClassRule
    public static WireMockClassRule server = new WireMockClassRule(wireMockConfig().dynamicPort());

    private UnsignedXrayClient client;

    @Before
    public void setUp() {
        client = new UnsignedXrayClient(server.baseUrl());
    }

    @Test
    public void getSamplingRules() throws Exception {
        GetSamplingRulesResult expected = new GetSamplingRulesResult();
        expected.setNextToken("nexttoken");

        stubFor(any(anyUrl()).willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody(OBJECT_MAPPER.writeValueAsBytes(expected))));

        GetSamplingRulesResult result = client.getSamplingRules(new GetSamplingRulesRequest());

        assertThat(expected).isEqualTo(result);

        verify(postRequestedFor(urlEqualTo("/GetSamplingRules"))
                       .withHeader("Content-Type", equalTo("application/json"))
                       .withRequestBody(equalToJson("{}")));
    }

    @Test
    public void getSamplingTargets() throws Exception {
        GetSamplingTargetsResult expected = new GetSamplingTargetsResult();
        expected.setLastRuleModification(new Date(1000));

        stubFor(any(anyUrl()).willReturn(aResponse()
                                                 .withStatus(200)
                                                 .withBody(OBJECT_MAPPER.writeValueAsBytes(expected))));

        GetSamplingTargetsRequest request = new GetSamplingTargetsRequest().
                withSamplingStatisticsDocuments(new SamplingStatisticsDocument().withClientID("client-id"));

        GetSamplingTargetsResult result = client.getSamplingTargets(request);

        assertThat(expected).isEqualTo(result);

        verify(postRequestedFor(urlEqualTo("/GetSamplingTargets"))
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
