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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

class MetadataUtils {
    private static final Log logger = LogFactory.getLog(MetadataUtils.class);

    private static final int CONNECT_TIMEOUT_MILLIS = 100;
    private static final int READ_TIMEOUT_MILLIS = 1000;

    private MetadataUtils() {
    }

    static String fetchString(String httpMethod, URL url, String token, boolean includeTtl, String metadataService) {
        final HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            logger.debug("Error connecting to " + metadataService, e);
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
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("X-aws-ec2-metadata-token", token);
        }

        final int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (Exception e) {
            if (e instanceof SocketTimeoutException) {
                logger.debug("Timed out trying to connect to " + metadataService);
            } else {
                logger.debug("Error connecting to " + metadataService, e);
            }
            return "";
        }

        if (responseCode != 200) {
            logger.warn("Error response from " + metadataService + ": code (" +
                responseCode + ") text " + readResponseString(connection));
        }

        return MetadataUtils.readResponseString(connection).trim();
    }

    static String readResponseString(HttpURLConnection connection) {
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

    static void readTo(@Nullable InputStream is, ByteArrayOutputStream os) throws IOException {
        if (is == null) {
            return;
        }
        int b;
        while ((b = is.read()) != -1) {
            os.write(b);
        }
    }
}
