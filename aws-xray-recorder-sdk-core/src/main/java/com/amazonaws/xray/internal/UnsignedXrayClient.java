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

import com.amazonaws.xray.config.DaemonConfiguration;
import com.amazonaws.xray.strategy.sampling.GetSamplingRulesRequest;
import com.amazonaws.xray.strategy.sampling.GetSamplingRulesResponse;
import com.amazonaws.xray.strategy.sampling.GetSamplingTargetsRequest;
import com.amazonaws.xray.strategy.sampling.GetSamplingTargetsResponse;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A simple client for sending API requests via the X-Ray daemon. Requests do not have to be
 * signed, so we can avoid having a strict dependency on the full AWS SDK in instrumentation. This
 * is an internal utility and not meant to represent the entire X-Ray API nor be particularly
 * efficient as we only use it in long poll loops.
 */
public class UnsignedXrayClient {

    private static final PropertyName HTTP_METHOD = PropertyName.construct("HTTPMethod");
    private static final PropertyName URL_PATH = PropertyName.construct("URLPath");

    // Visible for testing
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .registerModule(new SimpleModule().addDeserializer(Date.class, new FloatDateDeserializer()))
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public PropertyName findNameForDeserialization(Annotated a) {
                    if (a.getName().equals("hTTPMethod")) {
                        return HTTP_METHOD;
                    }
                    if (a.getName().equals("uRLPath")) {
                        return URL_PATH;
                    }
                    return super.findNameForDeserialization(a);
                }
            });
    private static final int TIME_OUT_MILLIS = 2000;

    private final URL getSamplingRulesEndpoint;
    private final URL getSamplingTargetsEndpoint;

    public UnsignedXrayClient() {
        this(new DaemonConfiguration().getEndpointForTCPConnection());
    }

    // Visible for testing
    UnsignedXrayClient(String endpoint) {
        try {
            getSamplingRulesEndpoint = new URL(endpoint + "/GetSamplingRules");
            getSamplingTargetsEndpoint = new URL(endpoint + "/SamplingTargets");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + endpoint, e);
        }
    }

    public GetSamplingRulesResponse getSamplingRules(GetSamplingRulesRequest request) {
        return sendRequest(getSamplingRulesEndpoint, request, GetSamplingRulesResponse.class);
    }

    public GetSamplingTargetsResponse getSamplingTargets(GetSamplingTargetsRequest request) {
        return sendRequest(getSamplingTargetsEndpoint, request, GetSamplingTargetsResponse.class);
    }

    private <T> T sendRequest(URL endpoint, Object request, Class<T> responseClass) {
        final HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) endpoint.openConnection();
        } catch (IOException e) {
            throw new XrayClientException("Could not connect to endpoint " + endpoint, e);
        }

        connection.setConnectTimeout(TIME_OUT_MILLIS);
        connection.setReadTimeout(TIME_OUT_MILLIS);

        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new IllegalStateException("Invalid protocol, can't happen.");
        }

        connection.addRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            OBJECT_MAPPER.writeValue(outputStream, request);
        } catch (IOException | IllegalArgumentException e) {
            throw new XrayClientException("Could not serialize and send request.", e);
        }

        final int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            throw new XrayClientException("Could not read response code.", e);
        }

        if (responseCode != 200) {
            throw new XrayClientException("Error response from X-Ray: " +
                                          readResponseString(connection));
        }

        try {
            return OBJECT_MAPPER.readValue(connection.getInputStream(), responseClass);
        } catch (IOException e) {
            throw new XrayClientException("Error reading response.", e);
        }
    }

    private static String readResponseString(HttpURLConnection connection) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (InputStream is = connection.getInputStream()) {
            readTo(is, os);
        } catch (IOException e) {
            // Only best effort read if we can.
        }
        try (InputStream is = connection.getErrorStream()) {
            readTo(is, os);
        } catch (IOException e) {
            // Only best effort read if we can.
        }
        try {
            return os.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported can't happen.");
        }
    }

    private static void readTo(@Nullable InputStream is, ByteArrayOutputStream os) throws IOException {
        // It is possible for getErrorStream to return null, though since we don't read it for success cases in practice it
        // shouldn't happen. Check just in case.
        if (is == null) {
            return;
        }
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
    }

    private static class FloatDateDeserializer extends StdDeserializer<Date> {

        private static final int AWS_DATE_MILLI_SECOND_PRECISION = 3;

        private FloatDateDeserializer() {
            super(Date.class);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return parseServiceSpecificDate(p.getText());
        }

        // Copied from AWS SDK https://github.com/aws/aws-sdk-java/blob/7b1e5b87b0bf03456df9e77716b14731adf9a7a7/aws-java-sdk-core/src/main/java/com/amazonaws/util/DateUtils.java#L239https://github.com/aws/aws-sdk-java/blob/7b1e5b87b0bf03456df9e77716b14731adf9a7a7/aws-java-sdk-core/src/main/java/com/amazonaws/util/DateUtils.java#L239
        /**
         * Parses the given date string returned by the AWS service into a Date
         * object.
         */
        private static Date parseServiceSpecificDate(String dateString) {
            try {
                BigDecimal dateValue = new BigDecimal(dateString);
                return new Date(dateValue.scaleByPowerOfTen(AWS_DATE_MILLI_SECOND_PRECISION).longValue());
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unable to parse date : " + dateString, nfe);
            }
        }
    }
}
