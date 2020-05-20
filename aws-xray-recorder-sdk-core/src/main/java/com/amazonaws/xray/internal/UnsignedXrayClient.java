package com.amazonaws.xray.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.services.xray.model.GetSamplingRulesRequest;
import com.amazonaws.services.xray.model.GetSamplingRulesResult;
import com.amazonaws.services.xray.model.GetSamplingTargetsRequest;
import com.amazonaws.services.xray.model.GetSamplingTargetsResult;
import com.amazonaws.xray.config.DaemonConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * A simple client for sending API requests via the X-Ray daemon. Requests do not have to be
 * signed, so we can avoid having a strict dependency on the full AWS SDK in instrumentation. This
 * is an internal utility and not meant to represent the entire X-Ray API nor be particularly
 * efficient as we only use it in long poll loops.
 */
public class UnsignedXrayClient {

    // Visible for testing
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_EMPTY)
            .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public boolean hasIgnoreMarker(AnnotatedMember m) {
                    // This is a somewhat hacky way of having ObjectMapper only serialize the fields in our
                    // model classes instead of the base class that comes from the SDK. In the future, we will
                    // remove the SDK dependency itself and the base classes and this hack will go away.
                    if (m.getDeclaringClass() == AmazonWebServiceRequest.class ||
                        m.getDeclaringClass() == AmazonWebServiceResult.class) {
                        return true;
                    }
                    return super.hasIgnoreMarker(m);
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
            getSamplingTargetsEndpoint = new URL(endpoint + "/GetSamplingTargets");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + endpoint, e);
        }
    }

    public GetSamplingRulesResult getSamplingRules(GetSamplingRulesRequest request) {
        return sendRequest(getSamplingRulesEndpoint, request, GetSamplingRulesResult.class);
    }

    public GetSamplingTargetsResult getSamplingTargets(GetSamplingTargetsRequest request) {
        return sendRequest(getSamplingTargetsEndpoint, request, GetSamplingTargetsResult.class);
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
        } catch (IOException e) {
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

    private static void readTo(InputStream is, ByteArrayOutputStream os) throws IOException {
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
    }
}
