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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A plugin, for use with the {@code AWSXRayRecorderBuilder} class, which will add Elastic Beanstalk environment information to
 * segments generated by the built {@code AWSXRayRecorder} instance.
 *
 * @see com.amazonaws.xray.AWSXRayRecorderBuilder#withPlugin(Plugin)
 *
 */
public class ElasticBeanstalkPlugin implements Plugin {
    public static final String ORIGIN = "AWS::ElasticBeanstalk::Environment";

    private static final Log logger =
        LogFactory.getLog(ElasticBeanstalkPlugin.class);

    @MonotonicNonNull
    private ObjectMapper OBJECT_MAPPER;

    private static final String CONF_PATH = "/var/elasticbeanstalk/xray/environment.conf";
    private static final String SERVICE_NAME = "elastic_beanstalk";

    private Map<String, Object> runtimeContext;

    public ElasticBeanstalkPlugin() {
        runtimeContext = new HashMap<>();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean isEnabled() {
        return Files.exists(Paths.get(CONF_PATH));
    }

    public void populateRuntimeContext() {
        byte[] manifestBytes = new byte[0];

        // lazily initialize for performance on unused plugins
        if (OBJECT_MAPPER == null) {
            OBJECT_MAPPER = new ObjectMapper();
        }

        try {
            manifestBytes = Files.readAllBytes(Paths.get(CONF_PATH));
        } catch (IOException | OutOfMemoryError | SecurityException e) {
            logger.warn("Unable to read Beanstalk configuration at path " + CONF_PATH + " : " + e.getMessage());
            return;
        }
        try {
            TypeReference<HashMap<String, Object>> typeReference = new TypeReference<HashMap<String, Object>>() {};
            runtimeContext = OBJECT_MAPPER.readValue(manifestBytes, typeReference);
        } catch (IOException e) {
            logger.warn("Unable to read Beanstalk configuration at path " + CONF_PATH + " : " + e.getMessage());
            return;
        }
    }

    @Override
    public Map<String, @Nullable Object> getRuntimeContext() {
        if (runtimeContext.isEmpty()) {
            populateRuntimeContext();
        }

        return (Map<String, @Nullable Object>) runtimeContext;
    }

    @Override
    public String getOrigin() {
        return ORIGIN;
    }

    /**
     * Determine equality of plugins using origin to uniquely identify them
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Plugin)) { return false; }
        return this.getOrigin().equals(((Plugin) o).getOrigin());
    }

    /**
     * Hash plugin object using origin to uniquely identify them
     */
    @Override
    public int hashCode() {
        return this.getOrigin().hashCode();
    }
}
