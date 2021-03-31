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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
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
import org.mockito.Spy;

class SqlSubsegmentsTest {
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

    @Spy
    WeakConcurrentMap<Connection, ConnectionInfo> mapSpy = new WeakConcurrentMap.WithInlinedExpunction<>();

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
        SqlSubsegments.setConnMap(new WeakConcurrentMap.WithInlinedExpunction<>());
    }

    @AfterEach
    void cleanup() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    void testCreateSubsegmentWithoutSql() {
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
    void testCreateSubsegmentWithSql() {
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

    @Test
    void testCreateSubsegmentWhenConnectionThrowsException() throws SQLException {
        when(connection.getMetaData()).thenThrow(new SQLException());

        Subsegment sub = SqlSubsegments.forQuery(connection, SQL);

        assertThat(AWSXRay.getCurrentSubsegment()).isEqualTo(sub);
        assertThat(sub.getName()).isEqualTo(SqlSubsegments.DEFAULT_DATABASE_NAME);
        assertThat(sub.isInProgress()).isTrue();
        assertThat(sub.getParentSegment().getSubsegments()).contains(sub);
    }

    @Test
    void testDbNameIsNotNull() throws SQLException {
        when(connection.getCatalog()).thenReturn(null);

        Subsegment sub = SqlSubsegments.forQuery(connection, SQL);

        assertThat(sub.getName()).isEqualTo(SqlSubsegments.DEFAULT_DATABASE_NAME + "@" + HOST);
    }

    @Test
    void testHostIsNotNull() throws SQLException {
        when(metaData.getURL()).thenReturn("some invalid URL");

        Subsegment sub = SqlSubsegments.forQuery(connection, SQL);

        assertThat(sub.getName()).isEqualTo(CATALOG);
    }

    @Test
    void testPrefersSubsegmentNameFromUrl() {
        WeakConcurrentMap<Connection, ConnectionInfo> map = new WeakConcurrentMap.WithInlinedExpunction<>();
        map.put(connection, new ConnectionInfo.Builder().dbName("newDb").host("another").build());
        SqlSubsegments.setConnMap(map);
    }

    @Test
    void testPrefersMetaDataFromUrl() {
        WeakConcurrentMap<Connection, ConnectionInfo> map = new WeakConcurrentMap.WithInlinedExpunction<>();
        map.put(connection, new ConnectionInfo.Builder()
            .sanitizedUrl("jdbc:oracle:rds.us-west-2.com").user("another").dbName("newDb").host("rds.us-west-2.com").build());
        SqlSubsegments.setConnMap(map);

        Subsegment sub = SqlSubsegments.forQuery(connection, SQL);

        assertThat(sub.getName()).isEqualTo("newDb@rds.us-west-2.com");
        assertThat(sub.getSql()).containsEntry("url", "jdbc:oracle:rds.us-west-2.com");
        assertThat(sub.getSql()).containsEntry("user", "another");
    }

    @Test
    void testCacheInsertionOnNewConnection() throws SQLException {
        when(metaData.getURL()).thenReturn("jdbc:oracle:thin@rds.us-west-2.com");
        SqlSubsegments.setConnMap(mapSpy);

        SqlSubsegments.forQuery(connection, "query 1");
        SqlSubsegments.forQuery(connection, "query 2");

        assertThat(mapSpy).hasSize(1);
        assertThat(mapSpy.containsKey(connection)).isTrue();

        verify(mapSpy, times(1)).put(eq(connection), any());
        verify(mapSpy, times(2)).get(connection);
    }
}
