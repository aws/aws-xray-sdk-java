package com.amazonaws.xray.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracingDataSourceTest {

    private DataSource dataSource;

    public interface OtherWrapper extends DataSource, ExtraInterface {
    }

    public interface ExtraInterface {
    }

    @Mock
    private OtherWrapper delegate;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws SQLException {
        dataSource = TracingDataSource.decorate(delegate);
        doReturn(false).when(delegate).isWrapperFor(any());
        doReturn(true).when(delegate).isWrapperFor(OtherWrapper.class);
        doReturn(true).when(delegate).isWrapperFor(ExtraInterface.class);
        doThrow(SQLException.class).when(delegate).unwrap(any());
        doReturn(delegate).when(delegate).unwrap(OtherWrapper.class);
        doReturn(delegate).when(delegate).unwrap(ExtraInterface.class);
    }

    @Test
    public void testDecoration() throws SQLException {
        assertTrue(dataSource instanceof TracingDataSource);
        assertTrue(dataSource.isWrapperFor(DataSource.class));
        assertTrue(dataSource.isWrapperFor(TracingDataSource.class));
        assertTrue(dataSource.isWrapperFor(OtherWrapper.class));
        assertTrue(dataSource.isWrapperFor(ExtraInterface.class));
        assertFalse(dataSource.isWrapperFor(Long.class));
        verify(delegate, never()).isWrapperFor(DataSource.class);
        verify(delegate, never()).isWrapperFor(TracingDataSource.class);
        verify(delegate).isWrapperFor(OtherWrapper.class);
        verify(delegate).isWrapperFor(ExtraInterface.class);
        verify(delegate).isWrapperFor(Long.class);
    }

    @Test
    public void testUnwrap() throws SQLException {
        Assert.assertSame(dataSource, dataSource.unwrap(DataSource.class));
        Assert.assertSame(dataSource, dataSource.unwrap(TracingDataSource.class));
        Assert.assertSame(delegate, dataSource.unwrap(OtherWrapper.class));
        Assert.assertSame(delegate, dataSource.unwrap(ExtraInterface.class));
        boolean exceptionThrown = false;
        try {
            dataSource.unwrap(Long.class);
        } catch (final SQLException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        verify(delegate, never()).unwrap(DataSource.class);
        verify(delegate, never()).unwrap(TracingDataSource.class);
        verify(delegate).unwrap(OtherWrapper.class);
        verify(delegate).unwrap(ExtraInterface.class);
        verify(delegate).unwrap(Long.class);
    };

    @Test
    public void testGetConnection() throws Exception {
        assertTrue(dataSource.getConnection() instanceof TracingConnection);
        verify(delegate).getConnection();

        assertTrue(dataSource.getConnection("foo", "bar") instanceof TracingConnection);
        verify(delegate).getConnection("foo", "bar");
    }
}
