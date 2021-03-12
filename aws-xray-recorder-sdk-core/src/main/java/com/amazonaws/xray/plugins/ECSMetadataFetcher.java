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
import org.checkerframework.checker.nullness.qual.Nullable;

class ECSMetadataFetcher {
    private static final Log logger = LogFactory.getLog(ECSMetadataFetcher.class);

    private static final String METADATA_SERVICE_NAME = "TMDE";
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    @Nullable
    private final URL containerUrl;

    // TODO: Record additional attributes in runtime context from Task Metadata Endpoint
    enum ECSContainerMetadata {
        LOG_DRIVER,
        LOG_GROUP_REGION,
        LOG_GROUP_NAME,
        CONTAINER_ARN,
    }

    ECSMetadataFetcher(@Nullable String endpoint) {
        if (endpoint == null) {
            this.containerUrl = null;
            return;
        }

        try {
            this.containerUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal endpoint: " + endpoint);
        }
    }

    Map<ECSContainerMetadata, String> fetchContainer() {
        if (this.containerUrl == null) {
            return Collections.emptyMap();
        }

        String metadata = MetadataUtils.fetchString("GET", this.containerUrl, "", false, METADATA_SERVICE_NAME);

        Map<ECSContainerMetadata, String> result = new HashMap<>();
        try (JsonParser parser = JSON_FACTORY.createParser(metadata)) {
            parser.nextToken();
            parseContainerJson(parser, result);
        } catch (IOException e) {
            logger.warn("Could not parse container metadata.", e);
            return Collections.emptyMap();
        }

        // This means the document didn't have all the metadata fields we wanted.
        if (result.size() != ECSContainerMetadata.values().length) {
            logger.debug("Container metadata response missing metadata: " + metadata);
        }

        return Collections.unmodifiableMap(result);
    }

    // Helper method to shallow-parse a JSON object, assuming the parser is located at the start of an object,
    // and record the desired fields in the result map in-place
    private void parseContainerJson(JsonParser parser, Map<ECSContainerMetadata, String> result) throws IOException {
        if (!parser.isExpectedStartObjectToken()) {
            logger.warn("Container metadata endpoint returned invalid JSON");
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String value = parser.nextTextValue();
            switch (parser.getCurrentName()) {
                case "LogDriver":
                    result.put(ECSContainerMetadata.LOG_DRIVER, value);
                    break;
                case "ContainerARN":
                    result.put(ECSContainerMetadata.CONTAINER_ARN, value);
                    break;
                case "awslogs-group":
                    result.put(ECSContainerMetadata.LOG_GROUP_NAME, value);
                    break;
                case "awslogs-region":
                    result.put(ECSContainerMetadata.LOG_GROUP_REGION, value);
                    break;
                case "LogOptions":
                    parseContainerJson(parser, result);  // Parse the LogOptions object for log fields
                    break;
                default:
                    parser.skipChildren();
            }
            if (result.size() == ECSContainerMetadata.values().length) {
                return;
            }
        }
    }
}
