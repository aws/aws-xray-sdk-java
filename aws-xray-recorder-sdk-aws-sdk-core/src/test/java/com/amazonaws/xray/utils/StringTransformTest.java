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
