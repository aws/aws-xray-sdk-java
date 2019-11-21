package com.amazonaws.xray.sql;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracingConnectionTest {

    private Connection connection;

    @Mock
    private Connection delegate;

    @Before
    public void setup() {
        connection = TracingConnection.decorate(delegate);
    }

    @Test
    public void testDecoration() {
        assertTrue(connection instanceof TracingConnection);
    }

    @Test
    public void testCreateStatement() throws Exception {
        assertTrue(connection.createStatement() instanceof Statement);
        verify(delegate).createStatement();

        assertTrue(connection.createStatement(2, 3) instanceof Statement);
        verify(delegate).createStatement(2, 3);

        assertTrue(connection.createStatement(2, 3, 4) instanceof Statement);
        verify(delegate).createStatement(2, 3, 4);
    }

    @Test
    public void testPrepareStatement() throws Exception {
        assertTrue(connection.prepareStatement("foo") instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo");

        assertTrue(connection.prepareStatement("foo", 2) instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo", 2);

        assertTrue(connection.prepareStatement("foo", new int[]{2, 3}) instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo", new int[]{2, 3});

        assertTrue(connection.prepareStatement("foo", new String[]{"bar", "baz"}) instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo", new String[]{"bar", "baz"});

        assertTrue(connection.prepareStatement("foo", 2, 3) instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo", 2, 3);

        assertTrue(connection.prepareStatement("foo", 2, 3, 4) instanceof PreparedStatement);
        verify(delegate).prepareStatement("foo", 2, 3, 4);
    }

    @Test
    public void testPrepareCall() throws Exception {
        assertTrue(connection.prepareCall("foo") instanceof CallableStatement);
        verify(delegate).prepareCall("foo");

        assertTrue(connection.prepareCall("foo", 2, 3) instanceof CallableStatement);
        verify(delegate).prepareCall("foo", 2, 3);

        assertTrue(connection.prepareCall("foo", 2, 3, 4) instanceof CallableStatement);
        verify(delegate).prepareCall("foo", 2, 3, 4);
    }
}
