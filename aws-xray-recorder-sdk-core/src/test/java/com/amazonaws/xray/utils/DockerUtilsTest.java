package com.amazonaws.xray.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DockerUtilsTest {
    private static final String DOCKER_ID = "79311de543c2b01bdbb7ccaf355e71a02b0726366a1427ecfceb5e1f5be81644";

    @Test
    public void testEmptyCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/emptyCgroup"));
        String id = dockerUtils.getContainerId();
        Assert.assertNull(id);
    }

    @Test
    public void testInvalidCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/invalidCgroup"));
        String id = dockerUtils.getContainerId();
        Assert.assertNull(id);
    }

    @Test
    public void testValidFirstLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/validCgroup"));
        String id = dockerUtils.getContainerId();
        Assert.assertEquals(DOCKER_ID, id);
    }

    @Test
    public void testValidLaterLineCgroupFile() throws IOException {
        DockerUtils dockerUtils = new DockerUtils(DockerUtilsTest.class.getResource("/com/amazonaws/xray/utils/validSecondCgroup"));
        String id = dockerUtils.getContainerId();
        Assert.assertEquals(DOCKER_ID, id);
    }
}
