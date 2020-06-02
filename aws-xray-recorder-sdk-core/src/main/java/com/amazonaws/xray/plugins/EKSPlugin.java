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
import com.amazonaws.xray.utils.ContainerInsightsUtil;
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


/**
 * A plugin, for use with the {@code AWSXRayRecorderBuilder} class, which will add Kubernetes metadata to segments.
 * If the cluster uses ContainerInsights this plugin will extract log configuration information.
 *
 * @see com.amazonaws.xray.AWSXRayRecorderBuilder#withPlugin(Plugin)
 *
 */
public class EKSPlugin implements Plugin {
    public static final String ORIGIN = "AWS::EKS::Container";

    private static final String SERVICE_NAME = "eks";
    private static final String POD_CONTEXT_KEY = "pod";
    private static final String CLUSTER_NAME_KEY = "cluster_name";
    private static final String CONTAINER_ID_KEY = "containerId";

    private static final Log logger = LogFactory.getLog(EKSPlugin.class);

    private String clusterName;
    private Map<String, Object> runtimeContext;
    private Set<AWSLogReference> logReferences;
    private DockerUtils dockerUtils;

    public EKSPlugin() {
        this(ContainerInsightsUtil.getClusterName());
    }

    public EKSPlugin(final String clusterName) {
        this.clusterName = clusterName;
        this.runtimeContext = new HashMap<>();
        this.logReferences = new HashSet<>();
        this.dockerUtils = new DockerUtils();
    }

    @Override
    public boolean isEnabled() {
        return ContainerInsightsUtil.isK8s();
    }

    @Override
    public Set<AWSLogReference> getLogReferences() {
        if (logReferences.isEmpty()) {
            populateLogReferences();
        }

        return logReferences;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, Object> getRuntimeContext() {
        if (runtimeContext.isEmpty()) {
            populateRuntimeContext();
        }

        return runtimeContext;
    }

    /**
     * Generate log references by calling K8s for Container Insights configuration data.
     */
    private void populateLogReferences() {
        if (clusterName == null) {
            clusterName = ContainerInsightsUtil.getClusterName();
        }

        AWSLogReference log = new AWSLogReference();
        log.setLogGroup(String.format("/aws/containerinsights/%s/application", clusterName));
        logReferences.add(log);
    }

    /**
     * Generate runtime context with pod metadata from K8s.
     */
    public void populateRuntimeContext() {
        if (clusterName == null) {
            clusterName = ContainerInsightsUtil.getClusterName();
        }

        runtimeContext.put(CLUSTER_NAME_KEY, clusterName);

        try {
            runtimeContext.put(CONTAINER_ID_KEY, dockerUtils.getContainerId());
        } catch (IOException e) {
            logger.error("Failed to read full container ID from kubernetes instance.", e);
        }

        try {
            runtimeContext.put(POD_CONTEXT_KEY, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhe) {
            logger.error("Could not get pod ID from hostname.", uhe);
        }
    }

    @Override
    public String getOrigin() {
        return ORIGIN;
    }

    @Override
    /**
     * Determine equality of plugins using origin to uniquely identify them
     */
    public boolean equals(Object o) {
        if (!(o instanceof Plugin)) { return false; }
        return this.getOrigin().equals(((Plugin) o).getOrigin());
    }

    @Override
    /**
     * Hash plugin object using origin to uniquely identify them
     */
    public int hashCode() {
        return this.getOrigin().hashCode();
    }
}
