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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class ECSPluginTest {
    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";
    private static final String GOOD_URI = "http://172.0.0.1";
    private static final String BAD_URI = "Not a URL";

    private final ECSPlugin plugin = new ECSPlugin();

    @Test
    @SetEnvironmentVariable(key = ECS_METADATA_KEY, value = GOOD_URI)
    public void testIsEnabled() {
        boolean enabled = plugin.isEnabled();
        assertThat(enabled).isTrue();
    }

    @Test
    @SetEnvironmentVariable(key = ECS_METADATA_KEY, value = BAD_URI)
    public void testNotEnabled() {
        boolean enabled = plugin.isEnabled();
        assertThat(enabled).isFalse();
    }

    @Test
    public void testNotEnabledWithoutEnvironmentVariable() {
        boolean enabled = plugin.isEnabled();
        assertThat(enabled).isFalse();
    }
}
