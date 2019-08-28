package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;
import org.junit.Test;
import java.util.Set;

import static org.junit.Assert.*;

public class EKSPluginTest {

    private static final String TEST_CLUSTER_NAME = "TestCluster";
    private static final String TEST_LOG_GROUP = "/aws/containerinsights/TestCluster/application";
    private static final String EXPECTERD_SERVICE_NAME = "eks";
    private static final String EXPECTED_ORIGIN = "AWS::EKS::Pod";

    private final EKSPlugin plugin = new EKSPlugin(TEST_CLUSTER_NAME, true);

    @Test
    public void testGenerationOfLogGroupName() {
        Set<AWSLogReference> references = plugin.getLogReferences();

        AWSLogReference reference = (AWSLogReference) references.toArray()[0];
        assertEquals(TEST_LOG_GROUP, reference.getLogGroup());
    }

    @Test
    public void testServiceName() {
        assertEquals(EXPECTERD_SERVICE_NAME, plugin.getServiceName());
    }

    @Test
    public void testOrigin() {
        assertEquals(EXPECTED_ORIGIN, plugin.getOrigin());
    }
}
