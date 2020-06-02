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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ECSPlugin.class)
public class ECSPluginTest {
    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";

    private final ECSPlugin plugin = new ECSPlugin();

    @Test
    public void testIsEnabled() {
        String uri = "http://172.0.0.1";
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(ECS_METADATA_KEY)).thenReturn(uri);

        boolean enabled = plugin.isEnabled();

        Assert.assertTrue(enabled);
    }

    @Test
    public void testNotEnabled() {
        String uri = "Not a URL";
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(ECS_METADATA_KEY)).thenReturn(uri);

        boolean enabled = plugin.isEnabled();

        Assert.assertFalse(enabled);
    }
}
