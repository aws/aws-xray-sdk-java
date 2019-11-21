package com.amazonaws.xray.plugins;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.ContainerInsightsUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ContainerInsightsUtil.class)
public class EKSPluginTest {

    private static final String TEST_CLUSTER_NAME = "TestCluster";
    private static final String TEST_LOG_GROUP = "/aws/containerinsights/TestCluster/application";
    private static final String EXPECTERD_SERVICE_NAME = "eks";
    private static final String EXPECTED_ORIGIN = "AWS::EKS::Container";

    private final EKSPlugin plugin = new EKSPlugin(TEST_CLUSTER_NAME);

    @Before
    public void setUpEKS() {
        PowerMockito.mockStatic(ContainerInsightsUtil.class);
    }

    @Test
    public void testInit() {
        BDDMockito.given(ContainerInsightsUtil.isK8s()).willReturn(true);

        Assert.assertTrue(plugin.isEnabled());
    }

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
