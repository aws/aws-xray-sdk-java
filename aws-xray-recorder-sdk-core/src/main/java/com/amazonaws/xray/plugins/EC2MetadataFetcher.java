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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class EC2MetadataFetcher {
    private static final Log logger = LogFactory.getLog(EC2MetadataFetcher.class);

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    enum EC2Metadata {
        INSTANCE_ID,
        AVAILABILITY_ZONE,
        INSTANCE_TYPE,
        AMI_ID,
    }

    private static final int TIMEOUT_MILLIS = 2000;
    private static final String DEFAULT_IMDS_ENDPOINT = "169.254.169.254";

    private final URL identityDocumentUrl;
    private final URL tokenUrl;

    EC2MetadataFetcher() {
        this(System.getenv("IMDS_ENDPOINT") != null ? System.getenv("IMDS_ENDPOINT") : DEFAULT_IMDS_ENDPOINT);
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
                    return result;
                }
            }
        } catch (IOException e) {
            logger.warn("Could not parse identity document.", e);
            return Collections.emptyMap();
        }

        // Getting here means the document didn't have all the metadata fields we wanted.
        logger.warn("Identity document missing metadata: " + identity);
        return result;
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

        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);

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
            logger.warn("Error connecting to IMDS.", e);
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

}
