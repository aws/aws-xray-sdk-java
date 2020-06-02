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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracingConnectionTest {

    private Connection connection;

    public interface OtherWrapper extends Connection, ExtraInterface {
    }

    public interface ExtraInterface {
    }


    @Mock
    private OtherWrapper delegate;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws SQLException {
        connection = TracingConnection.decorate(delegate);
        doReturn(false).when(delegate).isWrapperFor(any());
        doReturn(true).when(delegate).isWrapperFor(OtherWrapper.class);
        doReturn(true).when(delegate).isWrapperFor(ExtraInterface.class);
        doThrow(SQLException.class).when(delegate).unwrap(any());
        doReturn(delegate).when(delegate).unwrap(OtherWrapper.class);
        doReturn(delegate).when(delegate).unwrap(ExtraInterface.class);
    }

    @Test
    public void testDecoration() throws SQLException {
        assertTrue(connection instanceof TracingConnection);
        assertTrue(connection.isWrapperFor(Connection.class));
        assertTrue(connection.isWrapperFor(TracingConnection.class));
        assertTrue(connection.isWrapperFor(OtherWrapper.class));
        assertTrue(connection.isWrapperFor(ExtraInterface.class));
        assertFalse(connection.isWrapperFor(Long.class));
        verify(delegate, never()).isWrapperFor(Connection.class);
        verify(delegate, never()).isWrapperFor(TracingConnection.class);
        verify(delegate).isWrapperFor(OtherWrapper.class);
        verify(delegate).isWrapperFor(ExtraInterface.class);
        verify(delegate).isWrapperFor(Long.class);
    }

    @Test
    public void testUnwrap() throws SQLException {
        Assert.assertSame(connection, connection.unwrap(Connection.class));
        Assert.assertSame(connection, connection.unwrap(TracingConnection.class));
        Assert.assertSame(delegate, connection.unwrap(OtherWrapper.class));
        Assert.assertSame(delegate, connection.unwrap(ExtraInterface.class));
        boolean exceptionThrown = false;
        try {
            connection.unwrap(Long.class);
        } catch (final SQLException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        verify(delegate, never()).unwrap(Connection.class);
        verify(delegate, never()).unwrap(TracingConnection.class);
        verify(delegate).unwrap(OtherWrapper.class);
        verify(delegate).unwrap(ExtraInterface.class);
        verify(delegate).unwrap(Long.class);
    }

    @Test
    public void testCreateStatement() throws Exception {
        assertTrue(connection.createStatement() != null);
        verify(delegate).createStatement();

        assertTrue(connection.createStatement(2, 3) != null);
        verify(delegate).createStatement(2, 3);

        assertTrue(connection.createStatement(2, 3, 4) != null);
        verify(delegate).createStatement(2, 3, 4);
    }

    @Test
    public void testPrepareStatement() throws Exception {
        assertTrue(connection.prepareStatement("foo") != null);
        verify(delegate).prepareStatement("foo");

        assertTrue(connection.prepareStatement("foo", 2) != null);
        verify(delegate).prepareStatement("foo", 2);

        assertTrue(connection.prepareStatement("foo", new int[] {2, 3}) != null);
        verify(delegate).prepareStatement("foo", new int[]{2, 3});

        assertTrue(connection.prepareStatement("foo", new String[] {"bar", "baz"}) != null);
        verify(delegate).prepareStatement("foo", new String[]{"bar", "baz"});

        assertTrue(connection.prepareStatement("foo", 2, 3) != null);
        verify(delegate).prepareStatement("foo", 2, 3);

        assertTrue(connection.prepareStatement("foo", 2, 3, 4) != null);
        verify(delegate).prepareStatement("foo", 2, 3, 4);
    }

    @Test
    public void testPrepareCall() throws Exception {
        assertTrue(connection.prepareCall("foo") != null);
        verify(delegate).prepareCall("foo");

        assertTrue(connection.prepareCall("foo", 2, 3) != null);
        verify(delegate).prepareCall("foo", 2, 3);

        assertTrue(connection.prepareCall("foo", 2, 3, 4) != null);
        verify(delegate).prepareCall("foo", 2, 3, 4);
    }
}
