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

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class StringTransformTest {

    @Test
    public void testToSnakeCase() {
        Assert.assertEquals("table_name", StringTransform.toSnakeCase("tableName"));
        Assert.assertEquals("consumed_capacity", StringTransform.toSnakeCase("ConsumedCapacity"));
        Assert.assertEquals("item_collection_metrics", StringTransform.toSnakeCase("ItemCollectionMetrics"));
    }

    @Test
    public void testToSnakeCaseNoExtraUnderscores() {
        Assert.assertEquals("table_name", StringTransform.toSnakeCase("table_name"));
    }
}
