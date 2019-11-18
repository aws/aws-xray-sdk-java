package com.amazonaws.xray.sql;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracingDataSourceTest {

    private DataSource dataSource;

    @Mock
    private DataSource delegate;

    @Before
    public void setup() {
        dataSource = TracingDataSource.decorate(delegate);
    }

    @Test
    public void testDecoration() {
        assertTrue(dataSource instanceof TracingDataSource);
    }

    @Test
    public void testGetConnection() throws Exception {
        assertTrue(dataSource.getConnection() instanceof TracingConnection);
        verify(delegate).getConnection();

        assertTrue(dataSource.getConnection("foo", "bar") instanceof TracingConnection);
        verify(delegate).getConnection("foo", "bar");
    }
}
