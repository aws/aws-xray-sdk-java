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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.AWSLogReference;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DockerPluginTest {
    private DockerPlugin dockerPlugin;

    @Mock
    FileSystem fakeFs;

    @BeforeEach
    void setup() {
        dockerPlugin = new DockerPlugin(fakeFs);
    }

    @Test
    void testPluginEnabled() throws URISyntaxException {
        String path = "/com/amazonaws/xray/utils/easyDockerConfig.json";
        when(fakeFs.getPath(anyString())).thenReturn(Paths.get(DockerPluginTest.class.getResource(path).toURI()));

        assertThat(dockerPlugin.isEnabled()).isTrue();
    }

    @Test
    void testPluginDisabled() {
        when(fakeFs.getPath(anyString())).thenReturn(Paths.get("/some/definitely/nonexistent/path"));

        assertThat(dockerPlugin.isEnabled()).isFalse();
    }

    @Test
    void testDockerLogGroupDiscovery() throws URISyntaxException {
        String path = "/com/amazonaws/xray/utils/easyDockerConfig.json";
        when(fakeFs.getPath(anyString())).thenReturn(Paths.get(DockerPluginTest.class.getResource(path).toURI()));
        AWSLogReference expected = new AWSLogReference();
        expected.setLogGroup("docker-group");

        Set<AWSLogReference> logReferences = dockerPlugin.getLogReferences();
        assertThat(logReferences).hasSize(1);
        assertThat(logReferences).contains(expected);
    }

    @Test
    void testNoLogGroupsDiscovered() throws URISyntaxException {
        String path = "/com/amazonaws/xray/utils/noGroupDockerConfig.json";
        when(fakeFs.getPath(anyString())).thenReturn(Paths.get(DockerPluginTest.class.getResource(path).toURI()));

        Set<AWSLogReference> logReferences = dockerPlugin.getLogReferences();
        assertThat(logReferences).isEmpty();
    }
}
