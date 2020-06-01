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

package com.amazonaws.xray.handlers.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.net.URL;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.runners.MockitoJUnitRunner;

@FixMethodOrder(MethodSorters.JVM)
@RunWith(MockitoJUnitRunner.class)
public class AWSServiceHandlerManifestTest {
    private static URL testParameterWhitelist =
        AWSServiceHandlerManifestTest.class.getResource("/com/amazonaws/xray/handlers/config/OperationParameterWhitelist.json");

    private ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    @Test
    public void testOperationRequestDescriptors() throws Exception {
        AWSServiceHandlerManifest serviceManifest = mapper.readValue(testParameterWhitelist, AWSServiceHandlerManifest.class);
        AWSOperationHandlerManifest operationManifest = serviceManifest.getOperationHandlerManifest("DynamoDb");
        AWSOperationHandler operationHandler = operationManifest.getOperationHandler("BatchGetItem");
        AWSOperationHandlerRequestDescriptor descriptor = operationHandler.getRequestDescriptors().get("RequestItems");

        Assert.assertEquals(true, descriptor.isMap());
        Assert.assertEquals(false, descriptor.isList());
        Assert.assertEquals(true, descriptor.shouldGetKeys());
        Assert.assertEquals(false, descriptor.shouldGetCount());
        Assert.assertEquals("table_names", descriptor.getRenameTo());
    }

    @Test
    public void testOperationRequestParameters() throws Exception {
        AWSServiceHandlerManifest serviceManifest = mapper.readValue(testParameterWhitelist, AWSServiceHandlerManifest.class);
        AWSOperationHandlerManifest operationManifest = serviceManifest.getOperationHandlerManifest("DynamoDb");
        AWSOperationHandler operationHandler = operationManifest.getOperationHandler("CreateTable");
        HashSet<String> parameters = operationHandler.getRequestParameters();

        Assert.assertEquals(true, parameters.contains("GlobalSecondaryIndexes"));
        Assert.assertEquals(true, parameters.contains("LocalSecondaryIndexes"));
        Assert.assertEquals(true, parameters.contains("ProvisionedThroughput"));
        Assert.assertEquals(true, parameters.contains("TableName"));
    }

    @Test
    public void testOperationResponseDescriptors() throws Exception {
        AWSServiceHandlerManifest serviceManifest = mapper.readValue(testParameterWhitelist, AWSServiceHandlerManifest.class);
        AWSOperationHandlerManifest operationManifest = serviceManifest.getOperationHandlerManifest("DynamoDb");
        AWSOperationHandler operationHandler = operationManifest.getOperationHandler("ListTables");
        AWSOperationHandlerResponseDescriptor descriptor = operationHandler.getResponseDescriptors().get("TableNames");

        Assert.assertEquals(false, descriptor.isMap());
        Assert.assertEquals(true, descriptor.isList());
        Assert.assertEquals(false, descriptor.shouldGetKeys());
        Assert.assertEquals(true, descriptor.shouldGetCount());
        Assert.assertEquals("table_count", descriptor.getRenameTo());
    }

    @Test
    public void testOperationResponseParameters() throws Exception {
        AWSServiceHandlerManifest serviceManifest = mapper.readValue(testParameterWhitelist, AWSServiceHandlerManifest.class);
        AWSOperationHandlerManifest operationManifest = serviceManifest.getOperationHandlerManifest("DynamoDb");
        AWSOperationHandler operationHandler = operationManifest.getOperationHandler("DeleteItem");
        HashSet<String> parameters = operationHandler.getResponseParameters();

        Assert.assertEquals(true, parameters.contains("ConsumedCapacity"));
        Assert.assertEquals(true, parameters.contains("ItemCollectionMetrics"));
    }
}
