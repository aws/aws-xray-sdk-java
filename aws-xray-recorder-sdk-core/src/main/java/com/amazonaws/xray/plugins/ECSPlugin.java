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

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.DockerUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    private static final String ECS_METADATA_V3_KEY = "ECS_CONTAINER_METADATA_URI";
    private static final String ECS_METADATA_V4_KEY = "ECS_CONTAINER_METADATA_URI_V4";
    private static final String CONTAINER_ID_KEY = "container_id";
    private static final String CONTAINER_NAME_KEY = "container";
    private static final String CONTAINER_ARN_KEY = "container_arn";

    private final ECSMetadataFetcher fetcher;
    private final HashMap<String, @Nullable Object> runtimeContext;
    private final DockerUtils dockerUtils;
    private final Set<AWSLogReference> logReferences;
    private final Map<ECSMetadataFetcher.ECSContainerMetadata, String> containerMetadata;

    @SuppressWarnings("nullness:method.invocation.invalid")
    public ECSPlugin() {
        runtimeContext = new HashMap<>();
        dockerUtils = new DockerUtils();
        logReferences = new HashSet<>();
        fetcher = new ECSMetadataFetcher(getTmdeFromEnv());
        containerMetadata = this.fetcher.fetchContainer();
    }

    // Exposed for testing
    ECSPlugin(ECSMetadataFetcher fetcher) {
        runtimeContext = new HashMap<>();
        dockerUtils = new DockerUtils();
        logReferences = new HashSet<>();
        this.fetcher = fetcher;
        containerMetadata = this.fetcher.fetchContainer();
    }

    /**
     * Returns true if the environment variable added by ECS is present and contains a valid URI
     */
    @Override
    public boolean isEnabled() {
        String ecsMetadataUri = getTmdeFromEnv();
        return ecsMetadataUri != null && ecsMetadataUri.startsWith("http://");
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    public void populateRuntimeContext() {
        try {
            runtimeContext.put(CONTAINER_NAME_KEY, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhe) {
            logger.error("Could not get docker container ID from hostname.", uhe);
        }

        try {
            runtimeContext.put(CONTAINER_ID_KEY, dockerUtils.getContainerId());
        } catch (IOException e) {
            logger.error("Failed to read full container ID from container instance.", e);
        }

        if (containerMetadata.containsKey(ECSMetadataFetcher.ECSContainerMetadata.CONTAINER_ARN)) {
            runtimeContext.put(CONTAINER_ARN_KEY, containerMetadata.get(ECSMetadataFetcher.ECSContainerMetadata.CONTAINER_ARN));
        }
    }

    @Override
    public Map<String, @Nullable Object> getRuntimeContext() {
        populateRuntimeContext();
        return runtimeContext;
    }

    @Override
    public Set<AWSLogReference> getLogReferences() {
        if (logReferences.isEmpty()) {
            populateLogReferences();
        }

        return logReferences;
    }

    // See: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-account-settings.html#ecs-resource-ids
    private void populateLogReferences() {
        String logGroup = containerMetadata.get(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_NAME);
        if (logGroup == null) {
            return;
        }
        AWSLogReference logReference = new AWSLogReference();
        logReference.setLogGroup(logGroup);

        String logRegion = containerMetadata.get(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_REGION);
        String containerArn = containerMetadata.get(ECSMetadataFetcher.ECSContainerMetadata.CONTAINER_ARN);
        String logAccount = containerArn != null ? containerArn.split(":")[4] : null;

        if (logRegion != null && logAccount != null) {
            logReference.setArn("arn:aws:logs:" + logRegion + ":" + logAccount + ":log-group:" + logGroup);
        }
        logReferences.add(logReference);
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

    /**
     * @return V4 Metadata endpoint if present, otherwise V3 endpoint if present, otherwise null
     */
    @Nullable
    private String getTmdeFromEnv() {
        String ecsMetadataUri = System.getenv(ECS_METADATA_V4_KEY);
        if (ecsMetadataUri == null) {
            ecsMetadataUri = System.getenv(ECS_METADATA_V3_KEY);
        }

        return ecsMetadataUri;
    }
}
