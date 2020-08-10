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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

class EC2MetadataFetcher {
    private static final Log logger = LogFactory.getLog(EC2MetadataFetcher.class);

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    enum EC2Metadata {
        INSTANCE_ID,
        AVAILABILITY_ZONE,
        INSTANCE_TYPE,
        AMI_ID,
    }

    private static final int CONNECT_TIMEOUT_MILLIS = 100;
    private static final int READ_TIMEOUT_MILLIS = 1000;
    private static final String DEFAULT_IMDS_ENDPOINT = "169.254.169.254";

    private final URL identityDocumentUrl;
    private final URL tokenUrl;

    EC2MetadataFetcher() {
        this(getEndpoint());
    }

    EC2MetadataFetcher(String endpoint) {
        String urlBase = "http://" + endpoint;
        try {
            this.identityDocumentUrl = new URL(urlBase + "/latest/dynamic/instance-identity/document");
            this.tokenUrl = new URL(urlBase + "/latest/api/token");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal endpoint: " + endpoint);
        }
    }

    Map<EC2Metadata, String> fetch() {
        String token = fetchToken();

        // If token is empty, either IMDSv2 isn't enabled or an unexpected failure happened. We can still get
        // data if IMDSv1 is enabled.
        String identity = fetchIdentity(token);
        if (identity.isEmpty()) {
            // If no identity document, assume we are not actually running on EC2.
            return Collections.emptyMap();
        }

        Map<EC2Metadata, String> result = new HashMap<>();
        try (JsonParser parser = JSON_FACTORY.createParser(identity)) {
            parser.nextToken();

            if (!parser.isExpectedStartObjectToken()) {
                throw new IOException("Invalid JSON:" + identity);
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String value = parser.nextTextValue();
                switch (parser.getCurrentName()) {
                    case "instanceId":
                        result.put(EC2Metadata.INSTANCE_ID, value);
                        break;
                    case "availabilityZone":
                        result.put(EC2Metadata.AVAILABILITY_ZONE, value);
                        break;
                    case "instanceType":
                        result.put(EC2Metadata.INSTANCE_TYPE, value);
                        break;
                    case "imageId":
                        result.put(EC2Metadata.AMI_ID, value);
                        break;
                    default:
                        parser.skipChildren();
                }
                if (result.size() == EC2Metadata.values().length) {
                    return Collections.unmodifiableMap(result);
                }
            }
        } catch (IOException e) {
            logger.warn("Could not parse identity document.", e);
            return Collections.emptyMap();
        }

        // Getting here means the document didn't have all the metadata fields we wanted.
        logger.warn("Identity document missing metadata: " + identity);
        return Collections.unmodifiableMap(result);
    }

    private String fetchToken() {
        return fetchString("PUT", tokenUrl, "", true);
    }

    private String fetchIdentity(String token) {
        return fetchString("GET", identityDocumentUrl, token, false);
    }

    // Generic HTTP fetch function for IMDS.
    private static String fetchString(String httpMethod, URL url, String token, boolean includeTtl) {
        final HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            logger.warn("Error connecting to IMDS.", e);
            return "";
        }

        try {
            connection.setRequestMethod(httpMethod);
        } catch (ProtocolException e) {
            logger.warn("Unknown HTTP method, this is a programming bug.", e);
            return "";
        }

        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);

        if (includeTtl) {
            connection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "60");
        }
        if (!token.isEmpty()) {
            connection.setRequestProperty("X-aws-ec2-metadata-token", token);
        }

        final int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (Exception e) {
            if (e instanceof SocketTimeoutException) {
                logger.debug("Timed out trying to connect to IMDS, likely not operating in EC2 environment");
            } else {
                logger.warn("Error connecting to IMDS.", e);
            }
            return "";
        }

        if (responseCode != 200) {
            logger.warn("Error reponse from IMDS: code (" + responseCode + ") text " + readResponseString(connection));
        }

        return readResponseString(connection).trim();
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
        if (is == null) {
            return;
        }
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
    }

    private static String getEndpoint() {
        String endpointFromEnv = System.getenv("IMDS_ENDPOINT");
        return endpointFromEnv != null ? endpointFromEnv :  DEFAULT_IMDS_ENDPOINT;
    }
}
