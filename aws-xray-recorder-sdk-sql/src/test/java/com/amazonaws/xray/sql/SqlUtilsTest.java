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

package com.amazonaws.xray.sql;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SqlUtilsTest {
    private static final String URL = "http://www.foo.com";
    private static final String HOST = "www.foo.com";
    private static final String USER = "user";
    private static final String DRIVER_VERSION = "driver_version";
    private static final String DB_TYPE = "db_version";
    private static final String DB_VERSION = "db_version";
    private static final String SQL = "sql";
    private static final String CATALOG = "catalog";
    private Map<String, Object> expectedSqlParams;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Before
    public void setup() throws SQLException {
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(CATALOG);
        when(metaData.getURL()).thenReturn(URL);
        when(metaData.getUserName()).thenReturn(USER);
        when(metaData.getDriverVersion()).thenReturn(DRIVER_VERSION);
        when(metaData.getDatabaseProductName()).thenReturn(DB_TYPE);
        when(metaData.getDatabaseProductVersion()).thenReturn(DB_VERSION);

        expectedSqlParams = new HashMap<>();
        expectedSqlParams.put("url", URL);
        expectedSqlParams.put("user", USER);
        expectedSqlParams.put("driver_version", DRIVER_VERSION);
        expectedSqlParams.put("database_type", DB_TYPE);
        expectedSqlParams.put("database_version", DB_VERSION);

        AWSXRay.beginSegment("test");
    }

    @After
    public void cleanup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testCreateSubsegmentWithoutSql() throws SQLException {
        Subsegment sub = SqlUtils.createSqlSubsegment(connection, null);
        verifySqlSubsegment(sub);
    }

    @Test
    public void testCreateSubsegmentWithSql() throws SQLException {
        Subsegment sub = SqlUtils.createSqlSubsegment(connection, SQL);
        expectedSqlParams.put("sanitized_query", SQL);
        verifySqlSubsegment(sub);
    }

    private void verifySqlSubsegment(Subsegment sub) {
        assertEquals(CATALOG + "@" + HOST, sub.getName());
        assertEquals(Namespace.REMOTE.toString(), sub.getNamespace());
        assertEquals(expectedSqlParams, sub.getSql());
    }
}
