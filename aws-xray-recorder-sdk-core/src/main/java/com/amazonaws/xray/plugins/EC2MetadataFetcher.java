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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    private static final String METADATA_SERVICE_NAME = "IMDS";
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
        return MetadataUtils.fetchString("PUT", tokenUrl, "", true, METADATA_SERVICE_NAME);
    }

    private String fetchIdentity(String token) {
        return MetadataUtils.fetchString("GET", identityDocumentUrl, token, false, METADATA_SERVICE_NAME);
    }

    private static String getEndpoint() {
        String endpointFromEnv = System.getenv("IMDS_ENDPOINT");
        return endpointFromEnv != null ? endpointFromEnv :  DEFAULT_IMDS_ENDPOINT;
    }
}
