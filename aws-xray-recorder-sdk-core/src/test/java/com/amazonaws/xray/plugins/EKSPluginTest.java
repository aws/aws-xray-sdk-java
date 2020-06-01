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

import static org.junit.Assert.assertEquals;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.ContainerInsightsUtil;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
