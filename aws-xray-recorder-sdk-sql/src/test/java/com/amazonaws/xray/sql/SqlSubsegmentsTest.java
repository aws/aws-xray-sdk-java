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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SqlSubsegmentsTest {
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

    @BeforeEach
    void setup() throws SQLException {
        MockitoAnnotations.initMocks(this);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.getCatalog()).thenReturn(CATALOG);
        when(metaData.getURL()).thenReturn(URL);
        when(metaData.getUserName()).thenReturn(USER);
        when(metaData.getDriverVersion()).thenReturn(DRIVER_VERSION);
        when(metaData.getDatabaseProductName()).thenReturn(DB_TYPE);
        when(metaData.getDatabaseProductVersion()).thenReturn(DB_VERSION);

        AWSXRay.beginSegment("test");
    }

    @AfterEach
    void cleanup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testCreateSubsegmentWithoutSql() throws SQLException {
        expectedSqlParams = new HashMap<>();
        expectedSqlParams.put("url", URL);
        expectedSqlParams.put("user", USER);
        expectedSqlParams.put("driver_version", DRIVER_VERSION);
        expectedSqlParams.put("database_type", DB_TYPE);
        expectedSqlParams.put("database_version", DB_VERSION);

        Subsegment sub = SqlSubsegments.forQuery(connection, null);

        assertThat(sub.getName()).isEqualTo(CATALOG + "@" + HOST);
        assertThat(sub.getNamespace()).isEqualTo(Namespace.REMOTE.toString());
        assertThat(sub.getSql()).containsAllEntriesOf(expectedSqlParams);
    }

    @Test
    void testCreateSubsegmentWithSql() throws SQLException {
        expectedSqlParams = new HashMap<>();
        expectedSqlParams.put("url", URL);
        expectedSqlParams.put("user", USER);
        expectedSqlParams.put("driver_version", DRIVER_VERSION);
        expectedSqlParams.put("database_type", DB_TYPE);
        expectedSqlParams.put("database_version", DB_VERSION);
        expectedSqlParams.put("sanitized_query", SQL);

        Subsegment sub = SqlSubsegments.forQuery(connection, SQL);

        assertThat(sub.getName()).isEqualTo(CATALOG + "@" + HOST);
        assertThat(sub.getNamespace()).isEqualTo(Namespace.REMOTE.toString());
        assertThat(sub.getSql()).containsAllEntriesOf(expectedSqlParams);
    }
}
