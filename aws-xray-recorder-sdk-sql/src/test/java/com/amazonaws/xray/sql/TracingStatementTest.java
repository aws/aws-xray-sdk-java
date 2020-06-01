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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.strategy.ContextMissingStrategy;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracingStatementTest {

    private static final String URL = "http://www.foo.com";
    private static final String HOST = "www.foo.com";
    private static final String USER = "user";
    private static final String DRIVER_VERSION = "driver_version";
    private static final String DB_TYPE = "db_version";
    private static final String DB_VERSION = "db_version";
    private static final String SQL = "sql";
    private static final String CATALOG = "catalog";

    private Statement statement;
    private PreparedStatement preparedStatement;
    private CallableStatement callableStatement;
    private Map<String, Object> expectedSqlParams;

    @Mock
    private Statement delegate;

    @Mock
    private PreparedStatement preparedDelegate;

    @Mock
    private CallableStatement callableDelegate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Before
    public void setup() throws Exception {
        statement = TracingStatement.decorateStatement(delegate);
        preparedStatement = TracingStatement.decoratePreparedStatement(preparedDelegate, SQL);
        callableStatement = TracingStatement.decorateCallableStatement(callableDelegate, SQL);
        when(delegate.getConnection()).thenReturn(connection);
        when(preparedDelegate.getConnection()).thenReturn(connection);
        when(callableDelegate.getConnection()).thenReturn(connection);
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
        AWSXRay.beginSegment("foo");
    }

    @After
    public void cleanUp() {
        AWSXRay.clearTraceEntity();
    }

    @Test
    public void testExecute() throws Exception {
        when(delegate.execute(SQL)).thenReturn(true);
        assertEquals(true, statement.execute(SQL));
        assertSubsegment();
    }

    @Test
    public void testExecuteWithAutoGeneratedKeys() throws Exception {
        when(delegate.execute(SQL, 2)).thenReturn(true);
        assertEquals(true, statement.execute(SQL, 2));
        assertSubsegment();
    }

    @Test
    public void testExecuteBatch() throws Exception {
        int[] result = {2, 3};
        when(delegate.executeBatch()).thenReturn(result);
        assertEquals(result, statement.executeBatch());
        assertSubsegment();
    }

    @Test
    public void testExecuteQuery() throws Exception {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(delegate.executeQuery(SQL)).thenReturn(resultSet);
        assertEquals(resultSet, statement.executeQuery(SQL));
        assertSubsegment();
    }

    @Test
    public void testExecuteUpdate() throws Exception {
        when(delegate.executeUpdate(SQL)).thenReturn(2);
        assertEquals(2, statement.executeUpdate(SQL));
        assertSubsegment();
    }

    @Test
    public void testExecuteUpdateWithAutoGeneratedKeys() throws Exception {
        when(delegate.executeUpdate(SQL, 3)).thenReturn(2);
        assertEquals(2, statement.executeUpdate(SQL, 3));
        assertSubsegment();
    }

    @Test
    public void testPreparedStatementExecuteQuery() throws Exception {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(preparedDelegate.executeQuery()).thenReturn(resultSet);
        assertEquals(resultSet, preparedStatement.executeQuery());
        assertSubsegment();
    }

    @Test
    public void testPreparedStatementExecuteUpdate() throws Exception {
        when(preparedDelegate.executeUpdate()).thenReturn(2);
        assertEquals(2, preparedStatement.executeUpdate());
        assertSubsegment();
    }

    @Test
    public void testCalledStatementExecute() throws Exception {
        when(callableDelegate.execute()).thenReturn(true);
        assertEquals(true, callableStatement.execute());
        assertSubsegment();
    }

    @Test
    public void testCalledStatementExecuteQuery() throws Exception {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(callableDelegate.executeQuery()).thenReturn(resultSet);
        assertEquals(resultSet, callableStatement.executeQuery());
        assertSubsegment();
    }

    @Test
    public void testCalledStatementExecuteUpdate() throws Exception {
        when(callableDelegate.executeUpdate()).thenReturn(2);
        assertEquals(2, callableStatement.executeUpdate());
        assertSubsegment();
    }

    @Test
    public void testCaptureRuntimeException() throws Exception {
        Throwable exception = new RuntimeException("foo");
        when(delegate.execute(SQL)).thenThrow(exception);
        try {
            statement.execute(SQL);
            fail("Expected exception is not thrown");
        } catch (Throwable th) {
            assertEquals(exception, th);
        } finally {
            assertEquals(exception, AWSXRay.getCurrentSegment().getSubsegments().get(0).getCause().getExceptions().get(0)
                                           .getThrowable());
            assertSubsegment();
        }
    }

    @Test
    public void testCaptureSqlException() throws Exception {
        Throwable exception = new SQLException("foo");
        when(delegate.execute(SQL)).thenThrow(exception);
        try {
            statement.execute(SQL);
            fail("Expected exception is not thrown");
        } catch (Throwable th) {
            assertEquals(exception, th);
        } finally {
            assertEquals(exception, AWSXRay.getCurrentSegment().getSubsegments().get(0).getCause().getExceptions().get(0)
                                           .getThrowable());
            assertSubsegment();
        }
    }

    @Test
    public void testCaptureRuntimeExceptionWithoutSegment() throws Exception {
        ContextMissingStrategy oldStrategy = AWSXRay.getGlobalRecorder().getContextMissingStrategy();
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        try {
            Throwable exception = new RuntimeException("foo");
            when(delegate.execute(SQL)).thenThrow(exception);
            try {
                statement.execute(SQL);
                fail("Expected exception is not thrown");
            } catch (Throwable th) {
                assertEquals(exception, th);
            }
        } finally {
            AWSXRay.getGlobalRecorder().setContextMissingStrategy(oldStrategy);
        }
    }

    @Test
    public void testCaptureSqlExceptionWithoutSegment() throws Exception {
        ContextMissingStrategy oldStrategy = AWSXRay.getGlobalRecorder().getContextMissingStrategy();
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
        try {
            Throwable exception = new SQLException("foo");
            when(delegate.execute(SQL)).thenThrow(exception);
            try {
                statement.execute(SQL);
                fail("Expected exception is not thrown");
            } catch (Throwable th) {
                assertEquals(exception, th);
            }
        } finally {
            AWSXRay.getGlobalRecorder().setContextMissingStrategy(oldStrategy);
        }
    }

    @Test
    public void testNonTracedMethod() throws Exception {
        statement.close();
        verify(delegate).close();
        assertEquals(0, AWSXRay.getCurrentSegment().getSubsegments().size());
    }

    private void assertSubsegment() {
        Subsegment subsegment = AWSXRay.getCurrentSegment().getSubsegments().get(0);
        assertEquals(CATALOG + "@" + HOST, subsegment.getName());
        assertEquals(Namespace.REMOTE.toString(), subsegment.getNamespace());
        assertEquals(expectedSqlParams, subsegment.getSql());
    }
}
