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
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.AWSLogReference;
import com.amazonaws.xray.utils.JsonUtils;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonUtils.class})
public class EC2PluginTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private FileSystem fakeFs;

    @Mock
    private EC2MetadataFetcher metadataFetcher;

    private EC2Plugin ec2Plugin;

    @Before
    public void setUpEC2Plugin() {
        Map<EC2MetadataFetcher.EC2Metadata, String> metadata = new HashMap<>();
        metadata.put(EC2MetadataFetcher.EC2Metadata.INSTANCE_ID, "instance-1234");
        metadata.put(EC2MetadataFetcher.EC2Metadata.AVAILABILITY_ZONE, "ap-northeast-1a");
        metadata.put(EC2MetadataFetcher.EC2Metadata.INSTANCE_TYPE, "m4.xlarge");
        metadata.put(EC2MetadataFetcher.EC2Metadata.AMI_ID, "ami-1234");
        PowerMockito.mockStatic(JsonUtils.class);
        when(metadataFetcher.fetch()).thenReturn(metadata);
        ec2Plugin = new EC2Plugin(fakeFs, metadataFetcher);
    }

    @Test
    public void testMetadataPresent() {
        assertThat(ec2Plugin.isEnabled()).isTrue();

        ec2Plugin.populateRuntimeContext();
        assertThat(ec2Plugin.getRuntimeContext())
            .containsEntry("instance_id", "instance-1234")
            .containsEntry("availability_zone", "ap-northeast-1a")
            .containsEntry("instance_size", "m4.xlarge")
            .containsEntry("ami_id", "ami-1234");
    }

    @Test
    public void testMetadataNotPresent() {
        when(metadataFetcher.fetch()).thenReturn(Collections.emptyMap());
        ec2Plugin = new EC2Plugin(fakeFs, metadataFetcher);

        assertThat(ec2Plugin.isEnabled()).isFalse();
    }

    @Test
    public void testFilePathCreationFailure() {
        List<Path> pathList = new ArrayList<>();
        pathList.add(Paths.get("badRoot"));
        Mockito.doReturn(pathList).when(fakeFs).getRootDirectories();

        Set<AWSLogReference> logReferences = ec2Plugin.getLogReferences();

        assertThat(logReferences).isEmpty();
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

        assertThat(logReferences).hasOnlyOneElementSatisfying(
            reference -> assertThat(reference.getLogGroup()).isEqualTo("test_group"));
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

        assertThat(logReferences).hasSize(2);
    }
}
