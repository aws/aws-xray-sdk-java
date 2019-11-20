package com.amazonaws.xray.plugins;

import com.amazonaws.util.EC2MetadataUtils;
import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonUtils.class, EC2MetadataUtils.class})
public class EC2PluginTest {
    private EC2Plugin ec2Plugin;
    private FileSystem fakeFs = Mockito.mock(FileSystem.class);

    @Before
    public void setUpEC2Plugin() {
        PowerMockito.mockStatic(JsonUtils.class);
        PowerMockito.mockStatic(EC2MetadataUtils.class);
        ec2Plugin = new EC2Plugin(fakeFs);
    }

    @Test
    public void testInit() {
        BDDMockito.given(EC2MetadataUtils.getInstanceId()).willReturn("12345");

        Assert.assertTrue(ec2Plugin.isEnabled());
    }

    @Test
    public void testFilePathCreationFailure() {
        List<Path> pathList = new ArrayList<>();
        pathList.add(Paths.get("badRoot"));
        Mockito.doReturn(pathList).when(fakeFs).getRootDirectories();

        Set<AWSLogReference> logReferences = ec2Plugin.getLogReferences();

        Assert.assertTrue(logReferences.isEmpty());
    }

    @Test
    public void testGenerationOfLogReference() throws IOException {
        List<Path> pathList = new ArrayList<>();
        pathList.add(Paths.get("/"));
        Mockito.doReturn(pathList).when(fakeFs).getRootDirectories();


        List<String> groupList = new ArrayList<>();
        groupList.add("test_group");
        BDDMockito.given(JsonUtils.getMatchingListFromJsonArrayNode(Mockito.any(), Mockito.any())).willReturn(groupList);

        Set<AWSLogReference> logReferences = ec2Plugin.getLogReferences();
        AWSLogReference logReference = (AWSLogReference) logReferences.toArray()[0];

        Assert.assertEquals(1, logReferences.size());
        Assert.assertEquals("test_group", logReference.getLogGroup());
    }

    @Test
    public void testGenerationOfMultipleLogReferences() throws IOException {
        List<Path> pathList = new ArrayList<>();
        pathList.add(Paths.get("/"));
        Mockito.doReturn(pathList).when(fakeFs).getRootDirectories();

        List<String> groupList = new ArrayList<>();
        groupList.add("test_group1");
        groupList.add("test_group2");
        groupList.add("test_group1");  // intentionally same for deduping
        BDDMockito.given(JsonUtils.getMatchingListFromJsonArrayNode(Mockito.any(), Mockito.any())).willReturn(groupList);

        Set<AWSLogReference> logReferences = ec2Plugin.getLogReferences();

        Assert.assertEquals(2, logReferences.size());
    }
}
