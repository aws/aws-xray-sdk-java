package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.ContainerInsightsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A plugin, for use with the {@code AWSXRayRecorderBuilder} class, which will add Kubernetes metadata to segments.
 * If the cluster uses ContainerInsights this plugin will extract log configuration information.
 *
 * @see com.amazonaws.xray.AWSXRayRecorderBuilder#withPlugin(Plugin)
 *
 */
public class EKSPlugin implements Plugin {
    private static final String CI_APPLOG_GROUP_FORMAT="/aws/containerinsights/%s/application";
    private static final String ORIGIN = "AWS::EKS::Pod";
    private static final String SERVICE_NAME = "eks";
    private static final String POD_CONTEXT_KEY = "pod";
    private static final String CLUSTER_NAME_KEY = "cluster_name";

    private static final Log logger = LogFactory.getLog(EKSPlugin.class);

    private String clusterName;
    private Map<String, Object> runtimeContext;
    private Set<AWSLogReference> logReferences;
    private boolean enabled;

    public EKSPlugin() {
        this(ContainerInsightsUtil.getClusterName(), ContainerInsightsUtil.isK8s());
    }

    public EKSPlugin(final String clusterName, boolean enabled) {
        this.clusterName = clusterName;
        this.runtimeContext = new HashMap<>();
        this.logReferences = new HashSet<>();
        this.enabled = enabled;
    }

    @Override
    public Set<AWSLogReference> getLogReferences() {
        if(logReferences.isEmpty()) {
            populateLogReferences();
        }

        return logReferences;
    }

    @Override
    public String getOrigin() {
        return ORIGIN;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, Object> getRuntimeContext() {
        if(runtimeContext.isEmpty()) {
            populateRuntimeContext();
        }

        return runtimeContext;
    }

    /**
     * Generate log references by calling K8s for Container Insights configuration data.
     */
    private void populateLogReferences() {
        if (enabled) {
            if (clusterName == null) {
                clusterName = ContainerInsightsUtil.getClusterName();
            }

            AWSLogReference log = new AWSLogReference();
            log.setLogGroup(String.format(CI_APPLOG_GROUP_FORMAT, clusterName));
            logReferences.add(log);
        }
    }

    /**
     * Generate runtime context with pod metadata from K8s.
     */
    public void populateRuntimeContext() {
        if (enabled) {
            if (clusterName == null) {
                clusterName = ContainerInsightsUtil.getClusterName();
            }

            runtimeContext.put(CLUSTER_NAME_KEY, clusterName);

            try {
                runtimeContext.put(POD_CONTEXT_KEY, InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException uhe) {
                logger.error("Could not get pod ID from hostname.", uhe);
            }
        }
    }
}
