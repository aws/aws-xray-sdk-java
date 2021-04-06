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

package com.amazonaws.xray.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.AWSLogReference;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DockerUtilsTest {
    private static final String DOCKER_ID = "79311de543c2b01bdbb7ccaf355e71a02b0726366a1427ecfceb5e1f5be81644";

    @Mock
    private FileSystem mockFs;

    @Test
    void testEmptyCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/emptyCgroup"));
        String id = dockerUtils.getContainerId();
        assertThat(id).isNull();
    }

    @Test
    void testInvalidCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/invalidCgroup"));
        String id = dockerUtils.getContainerId();
        assertThat(id).isNull();
    }

    @Test
    void testValidFirstLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/validCgroup"));
        String id = dockerUtils.getContainerId();
        assertThat(id).isEqualTo(DOCKER_ID);
    }

    @Test
    void testValidLaterLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource(
            "/com/amazonaws/xray/utils/validSecondCgroup"));
        String id = dockerUtils.getContainerId();
        assertThat(id).isEqualTo(DOCKER_ID);
    }

    @Test
    void testParseBasicDockerConfig() throws URISyntaxException {
        String classPath = "/com/amazonaws/xray/utils/easyDockerConfig.json";
        when(mockFs.getPath(anyString())).thenReturn(Paths.get(DockerUtilsTest.class.getResource(classPath).toURI()));
        AWSLogReference expected = new AWSLogReference();
        expected.setLogGroup("docker-group");

        AWSLogReference logReference = DockerUtils.getAwsLogReference("", mockFs);
        assertThat(logReference).isEqualTo(expected);
    }

    @Test
    void testParseNonAwsConfig() throws URISyntaxException {
        String classPath = "/com/amazonaws/xray/utils/otherDriverDockerConfig.json";
        when(mockFs.getPath(anyString())).thenReturn(Paths.get(DockerUtilsTest.class.getResource(classPath).toURI()));

        AWSLogReference logReference = DockerUtils.getAwsLogReference("", mockFs);
        assertThat(logReference).isNull();
    }

    @Test
    void testParseDockerConfigWithoutGroup() throws URISyntaxException {
        String classPath = "/com/amazonaws/xray/utils/noGroupDockerConfig.json";
        when(mockFs.getPath(anyString())).thenReturn(Paths.get(DockerUtilsTest.class.getResource(classPath).toURI()));
        AWSLogReference expected = new AWSLogReference();

        AWSLogReference logReference = DockerUtils.getAwsLogReference("", mockFs);
        assertThat(logReference).isEqualTo(expected);
    }

    @Test
    void testNonExistentDockerConfig() {
        when(mockFs.getPath(anyString())).thenReturn(Paths.get("/some/definitely/fake/path"));
        AWSLogReference logReference = DockerUtils.getAwsLogReference("", mockFs);
        assertThat(logReference).isNull();
    }
}
