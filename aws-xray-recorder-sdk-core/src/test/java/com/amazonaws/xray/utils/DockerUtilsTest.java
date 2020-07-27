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

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DockerUtilsTest {
    private static final String DOCKER_ID = "79311de543c2b01bdbb7ccaf355e71a02b0726366a1427ecfceb5e1f5be81644";

    @Test
    void testEmptyCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/emptyCgroup"));
        String id = dockerUtils.getContainerId();
        Assertions.assertNull(id);
    }

    @Test
    void testInvalidCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/invalidCgroup"));
        String id = dockerUtils.getContainerId();
        Assertions.assertNull(id);
    }

    @Test
    void testValidFirstLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/validCgroup"));
        String id = dockerUtils.getContainerId();
        Assertions.assertEquals(DOCKER_ID, id);
    }

    @Test
    void testValidLaterLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource(
            "/com/amazonaws/xray/utils/validSecondCgroup"));
        String id = dockerUtils.getContainerId();
        Assertions.assertEquals(DOCKER_ID, id);
    }
}
