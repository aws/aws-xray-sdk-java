package com.amazonaws.xray.plugins;

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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class EC2MetadataFetcher {
    private static final Log logger = LogFactory.getLog(EC2MetadataFetcher.class);

    enum EC2Metadata {
        INSTANCE_ID,
        AVAILABILITY_ZONE,
        INSTANCE_TYPE,
        AMI_ID,
    }

    private static final int TIMEOUT_MILLIS = 2000;
    private static final String DEFAULT_IDMS_ENDPOINT = "169.254.169.254";

    private final URL tokenUrl;
    private final URL instanceIdUrl;
    private final URL availabilityZoneUrl;
    private final URL instanceTypeUrl;
    private final URL amiIdUrl;

    EC2MetadataFetcher() {
        this(System.getenv("IMDS_ENDPOINT") != null ? System.getenv("IMDS_ENDPOINT") : DEFAULT_IDMS_ENDPOINT);
    }

    EC2MetadataFetcher(String endpoint) {
        String urlBase = "http://" + endpoint;
        try {
            this.tokenUrl = new URL(urlBase + "/latest/api/token");
            this.instanceIdUrl = new URL(urlBase + "/latest/meta-data/instance-id");
            this.availabilityZoneUrl = new URL(urlBase + "/latest/meta-data/placement/availability-zone");
            this.instanceTypeUrl = new URL(urlBase + "/latest/meta-data/instance-type");
            this.amiIdUrl = new URL(urlBase + "/latest/meta-data/ami-id");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal endpoint: " + endpoint);
        }
    }

    Map<EC2Metadata, String> fetch() {
        String token = fetchToken();

        // If token is empty, either IDMSv2 isn't enabled or an unexpected failure happened. We can still get
        // data if IDMSv1 is enabled.
        String instanceId = fetchInstanceId(token);
        if (instanceId.isEmpty()) {
            // If no instance ID, assume we are not actually running on EC2.
            return Collections.emptyMap();
        }

        String availabilityZone = fetchAvailabilityZone(token);
        String instanceType = fetchInstanceType(token);
        String amiId = fetchAmiId(token);

        Map<EC2Metadata, String> result = new LinkedHashMap<>();
        result.put(EC2Metadata.INSTANCE_ID, instanceId);
        result.put(EC2Metadata.AVAILABILITY_ZONE, availabilityZone);
        result.put(EC2Metadata.INSTANCE_TYPE, instanceType);
        result.put(EC2Metadata.AMI_ID, amiId);
        return result;
    }

    private String fetchToken() {
        return fetchString("PUT", tokenUrl, "", true);
    }

    private String fetchInstanceId(String token) {
        return fetchString("GET", instanceIdUrl, token, false);
    }

    private String fetchAvailabilityZone(String token) {
        return fetchString("GET", availabilityZoneUrl, token, false);
    }

    private String fetchInstanceType(String token) {
        return fetchString("GET", instanceTypeUrl, token, false);
    }

    private String fetchAmiId(String token) {
        return fetchString("GET", amiIdUrl, token, false);
    }

    // Generic HTTP fetch function for IDMS.
    private static String fetchString(String httpMethod, URL url, String token, boolean includeTtl) {
        final HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            logger.warn("Error connecting to IDMS.", e);
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
            logger.warn("Error connecting to IDMS.", e);
            return "";
        }

        if (responseCode != 200) {
            logger.warn("Error reponse from IDMS: code (" + responseCode + ") text " + readResponseString(connection));
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
