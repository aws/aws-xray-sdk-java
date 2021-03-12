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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.AWSLogReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ECSPluginTest {
    private static final String ECS_METADATA_KEY = "ECS_CONTAINER_METADATA_URI";
    private static final String GOOD_URI = "http://172.0.0.1";
    private static final String BAD_URI = "Not a URL";

    @Mock
    private ECSMetadataFetcher mockFetcher;

    @Test
    @SetEnvironmentVariable(key = ECS_METADATA_KEY, value = GOOD_URI)
    void testIsEnabled() {
        ECSPlugin plugin = new ECSPlugin();
        assertThat(plugin.isEnabled()).isTrue();
    }

    @Test
    @SetEnvironmentVariable(key = ECS_METADATA_KEY, value = BAD_URI)
    void testBadUri() {
        assertThatThrownBy(() -> new ECSPlugin()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testNotEnabledWithoutEnvironmentVariable() {
        ECSPlugin plugin = new ECSPlugin();
        assertThat(plugin.isEnabled()).isFalse();
    }

    @Test
    void testLogGroupRecording() {
        Map<ECSMetadataFetcher.ECSContainerMetadata, String> containerMetadata = new HashMap<>();
        containerMetadata.put(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_REGION, "us-west-2");
        containerMetadata.put(ECSMetadataFetcher.ECSContainerMetadata.LOG_GROUP_NAME, "my-log-group");
        containerMetadata.put(ECSMetadataFetcher.ECSContainerMetadata.CONTAINER_ARN,
            "arn:aws:ecs:us-west-2:123456789012:container-instance/my-cluster/12345");
        when(mockFetcher.fetchContainer()).thenReturn(containerMetadata);

        ECSPlugin plugin = new ECSPlugin(mockFetcher);
        Set<AWSLogReference> references = plugin.getLogReferences();
        AWSLogReference expected = new AWSLogReference();
        expected.setLogGroup("my-log-group");
        expected.setArn("arn:aws:logs:us-west-2:123456789012:log-group:my-log-group");

        assertThat(references).containsOnly(expected);
    }
}
