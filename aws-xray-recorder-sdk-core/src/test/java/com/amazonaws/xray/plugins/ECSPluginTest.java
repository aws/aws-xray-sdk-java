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

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;

public class ECSPluginTest {
    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";
    private static final String GOOD_URI = "http://172.0.0.1";
    private static final String BAD_URI = "Not a URL";

    private final ECSPlugin plugin = new ECSPlugin();

    @BeforeEach
    public void setup() {
        environmentVariables.set(ECS_METADATA_KEY, null);
    }

    @Test
    public void testIsEnabled() {
        environmentVariables.set(ECS_METADATA_KEY, GOOD_URI);

        boolean enabled = plugin.isEnabled();
        assertThat(enabled).isTrue();
    }

    @Test
    public void testNotEnabled() {
        environmentVariables.set(ECS_METADATA_KEY, BAD_URI);

        boolean enabled = plugin.isEnabled();
        assertThat(enabled).isFalse();
    }
}
