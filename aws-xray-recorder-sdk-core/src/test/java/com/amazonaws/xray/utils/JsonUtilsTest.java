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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class JsonUtilsTest {
    private static final String SINGLE_LOG_CONFIG = "[{\"log_group_name\":\"test_group\"}]";
    private static final String MULTI_LOG_CONFIG = "[{\"log_group_name\":\"test_group1\"}, {\"log_group_name\":\"test_group2\"}, "
                                                   + "{\"log_group_name\":\"test_group1\"}]";
    private static final String SINGLE_LOG_CONFIG_WITH_STREAM = "[{\"log_group_name\":\"test_group\", "
                                                                + "\"log_stream_name\":\"test_stream\"}]";

    private static final String LOG_GROUP_NAME = "log_group_name";

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetLogGroup() throws IOException {
        JsonNode node = mapper.readTree(SINGLE_LOG_CONFIG);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(1, groupList.size());
        Assert.assertEquals("test_group", groupList.get(0));
    }

    @Test
    public void testGetMultipleLogGroups() throws IOException {
        JsonNode node = mapper.readTree(MULTI_LOG_CONFIG);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(3, groupList.size());
        Assert.assertTrue(groupList.contains("test_group1"));
        Assert.assertTrue(groupList.contains("test_group2"));
    }

    @Test
    public void testGetLogGroupWithStreamPresent() throws IOException {
        JsonNode node = mapper.readTree(SINGLE_LOG_CONFIG_WITH_STREAM);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(1, groupList.size());
        Assert.assertEquals("test_group", groupList.get(0));
    }
}
