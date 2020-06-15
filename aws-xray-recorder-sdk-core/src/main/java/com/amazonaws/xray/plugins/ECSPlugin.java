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

import com.amazonaws.xray.utils.DockerUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A plugin, for use with the {@code AWSXRayRecorderBuilder} class, which will add ECS container information to segments generated
 * by the built {@code AWSXRayRecorder} instance.
 * 
 * @see com.amazonaws.xray.AWSXRayRecorderBuilder#withPlugin(Plugin)
 *
 */
public class ECSPlugin implements Plugin {
    public static final String ORIGIN = "AWS::ECS::Container";

    private static final Log logger = LogFactory.getLog(ECSPlugin.class);

    private static final String SERVICE_NAME = "ecs";
    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";
    private static final String HTTP_PREFIX = "http://";
    private static final String CONTAINER_ID_KEY = "containerId";

    private final HashMap<String, Object> runtimeContext;
    private final DockerUtils dockerUtils;

    public ECSPlugin() {
        runtimeContext = new HashMap<>();
        dockerUtils = new DockerUtils();
    }

    /**
     * Returns true if the environment variable added by ECS is present and contains a valid URI
     */
    @Override
    public boolean isEnabled() {
        String ecsMetadataUri = System.getenv(ECS_METADATA_KEY);
        if (ecsMetadataUri == null) {
            return false;
        }

        return ecsMetadataUri.startsWith(HTTP_PREFIX);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    public void populateRuntimeContext() {
        try {
            runtimeContext.put("container", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhe) {
            logger.error("Could not get docker container ID from hostname.", uhe);
        }

        try {
            runtimeContext.put(CONTAINER_ID_KEY, dockerUtils.getContainerId());
        } catch (IOException e) {
            logger.error("Failed to read full container ID from container instance.", e);
        }
    }

    @Override
    public Map<String, Object> getRuntimeContext() {
        populateRuntimeContext();
        return runtimeContext;
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
