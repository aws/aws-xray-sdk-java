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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TracingDataSourceTest {

    private DataSource dataSource;

    public interface OtherWrapper extends DataSource, ExtraInterface {
    }

    public interface ExtraInterface {
    }

    @Mock
    private OtherWrapper delegate;

    @SuppressWarnings("unchecked")
    @BeforeEach
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
    void testDecoration() throws SQLException {
        Assertions.assertTrue(dataSource instanceof TracingDataSource);
        Assertions.assertTrue(dataSource.isWrapperFor(DataSource.class));
        Assertions.assertTrue(dataSource.isWrapperFor(TracingDataSource.class));
        Assertions.assertTrue(dataSource.isWrapperFor(OtherWrapper.class));
        Assertions.assertTrue(dataSource.isWrapperFor(ExtraInterface.class));
        Assertions.assertFalse(dataSource.isWrapperFor(Long.class));
        verify(delegate, never()).isWrapperFor(DataSource.class);
        verify(delegate, never()).isWrapperFor(TracingDataSource.class);
        verify(delegate).isWrapperFor(OtherWrapper.class);
        verify(delegate).isWrapperFor(ExtraInterface.class);
        verify(delegate).isWrapperFor(Long.class);
    }

    @Test
    void testUnwrap() throws SQLException {
        Assertions.assertSame(dataSource, dataSource.unwrap(DataSource.class));
        Assertions.assertSame(dataSource, dataSource.unwrap(TracingDataSource.class));
        Assertions.assertSame(delegate, dataSource.unwrap(OtherWrapper.class));
        Assertions.assertSame(delegate, dataSource.unwrap(ExtraInterface.class));
        boolean exceptionThrown = false;
        try {
            dataSource.unwrap(Long.class);
        } catch (final SQLException e) {
            exceptionThrown = true;
        }
        Assertions.assertTrue(exceptionThrown);
        verify(delegate, never()).unwrap(DataSource.class);
        verify(delegate, never()).unwrap(TracingDataSource.class);
        verify(delegate).unwrap(OtherWrapper.class);
        verify(delegate).unwrap(ExtraInterface.class);
        verify(delegate).unwrap(Long.class);
    }

    ;

    @Test
    void testGetConnection() throws Exception {
        Assertions.assertTrue(dataSource.getConnection() instanceof TracingConnection);
        verify(delegate).getConnection();

        Assertions.assertTrue(dataSource.getConnection("foo", "bar") instanceof TracingConnection);
        verify(delegate).getConnection("foo", "bar");
    }
}
