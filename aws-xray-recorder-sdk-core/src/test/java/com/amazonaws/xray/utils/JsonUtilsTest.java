package com.amazonaws.xray.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JsonUtilsTest {
    private static final String singleLogConfig = "[{\"log_group_name\":\"test_group\"}]";
    private static final String multiLogConfig = "[{\"log_group_name\":\"test_group1\"}, {\"log_group_name\":\"test_group2\"}, {\"log_group_name\":\"test_group1\"}]";
    private static final String singleLogConfigWithStream = "[{\"log_group_name\":\"test_group\", \"log_stream_name\":\"test_stream\"}]";

    private static final String LOG_GROUP_NAME = "log_group_name";

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetLogGroup() throws IOException {
        JsonNode node = mapper.readTree(singleLogConfig);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(1, groupList.size());
        Assert.assertEquals("test_group", groupList.get(0));
    }

    @Test
    public void testGetMultipleLogGroups() throws IOException {
        JsonNode node = mapper.readTree(multiLogConfig);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(3, groupList.size());
        Assert.assertTrue(groupList.contains("test_group1"));
        Assert.assertTrue(groupList.contains("test_group2"));
    }

    @Test
    public void testGetLogGroupWithStreamPresent() throws IOException {
        JsonNode node = mapper.readTree(singleLogConfigWithStream);

        List<String> groupList = JsonUtils.getMatchingListFromJsonArrayNode(node, LOG_GROUP_NAME);

        Assert.assertEquals(1, groupList.size());
        Assert.assertEquals("test_group", groupList.get(0));
    }
}
