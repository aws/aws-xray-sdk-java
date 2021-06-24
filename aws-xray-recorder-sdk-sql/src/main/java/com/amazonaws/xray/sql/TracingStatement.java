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

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @deprecated For internal use only.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@Deprecated
public class TracingStatement {

    private static final Log logger = LogFactory.getLog(TracingStatement.class);

    private static final boolean COLLECT_SQL_ENV =
            Boolean.parseBoolean(System.getenv("AWS_XRAY_COLLECT_SQL_QUERIES"));
    private static final boolean COLLECT_SQL_PROP =
            Boolean.parseBoolean(System.getProperty("com.amazonaws.xray.collectSqlQueries"));

    /**
     * Call {@code statement = TracingStatement.decorateStatement(statement)} to decorate your {@link Statement}
     * in order to have the queries recorded with an X-Ray Subsegment. Do not use the method on {@link PreparedStatement}
     * and {@link CallableStatement}. Use another two specific decorating method instead.
     *
     * @param statement the statement to decorate
     * @return a {@link Statement} that traces all SQL queries in X-Ray
     */
    public static Statement decorateStatement(Statement statement) {
        return (Statement) Proxy.newProxyInstance(TracingStatement.class.getClassLoader(),
                new Class[] { Statement.class },
                new TracingStatementHandler(statement, null));
    }

    /**
     * Call {@code preparedStatement = TracingStatement.decoratePreparedStatement(preparedStatement, sql)}
     * to decorate your {@link PreparedStatement} in order to have the queries recorded with an X-Ray Subsegment.
     *
     * @param statement the {@link PreparedStatement} to decorate
     * @param sql the sql query to execute
     * @return a {@link PreparedStatement} that traces all SQL queries in X-Ray
     */
    public static PreparedStatement decoratePreparedStatement(PreparedStatement statement, String sql) {
        return (PreparedStatement) Proxy.newProxyInstance(TracingStatement.class.getClassLoader(),
                new Class[] { PreparedStatement.class },
                new TracingStatementHandler(statement, sql));
    }

    /**
     * Call {@code callableStatement = TracingStatement.decorateCallableStatement(callableStatement, sql)}
     * to decorate your {@link CallableStatement}in order to have the queries recorded with an X-Ray Subsegment.
     *
     * @param statement the {@link CallableStatement} to decorate
     * @param sql the sql query to execute
     * @return a {@link CallableStatement} that traces all SQL queries in X-Ray
     */
    public static CallableStatement decorateCallableStatement(CallableStatement statement, String sql) {
        return (CallableStatement) Proxy.newProxyInstance(TracingStatement.class.getClassLoader(),
                new Class[] { CallableStatement.class },
                new TracingStatementHandler(statement, sql));
    }

    private static class TracingStatementHandler implements InvocationHandler {
        private static final String EXECUTE = "execute";
        private static final String EXECUTE_QUERY = "executeQuery";
        private static final String EXECUTE_UPDATE = "executeUpdate";
        private static final String EXECUTE_BATCH = "executeBatch";

        private final Statement delegate;

        private final String sql;

        TracingStatementHandler(Statement statement, String sql) {
            this.delegate = statement;
            this.sql = sql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Subsegment subsegment = null;

            if (isExecution(method)) {
                // only trace on execution methods
                subsegment = createSubsegment();
            }

            logger.debug(
                    String.format("Invoking statement execution with X-Ray tracing. Tracing active: %s", subsegment != null));
            try {
                // execute the query "wrapped" in a XRay Subsegment
                return method.invoke(delegate, args);
            } catch (Throwable t) {
                Throwable rootThrowable = t;
                if (t instanceof InvocationTargetException) {
                    // the reflection may wrap the actual error with an InvocationTargetException.
                    // we want to use the root cause to make the instrumentation seamless
                    InvocationTargetException ite = (InvocationTargetException) t;
                    if (ite.getTargetException() != null) {
                        rootThrowable = ite.getTargetException();
                    } else if (ite.getCause() != null) {
                        rootThrowable = ite.getCause();
                    }
                }

                if (subsegment != null) {
                    subsegment.addException(rootThrowable);
                }
                throw rootThrowable;
            } finally {
                if (subsegment != null && isExecution(method)) {
                    AWSXRay.endSubsegment();
                }
            }
        }

        private boolean isExecution(Method method) {
            return EXECUTE.equals(method.getName())
                    || EXECUTE_QUERY.equals(method.getName())
                    || EXECUTE_UPDATE.equals(method.getName())
                    || EXECUTE_BATCH.equals(method.getName());
        }

        private Subsegment createSubsegment() {
            try {
                return SqlSubsegments.forQuery(delegate.getConnection(), collectSqlQueries() ? sql : null);
            } catch (SQLException exception) {
                logger.warn("Failed to create X-Ray subsegment for the statement execution.", exception);
                return null;
            }
        }

        private boolean collectSqlQueries() {
            return COLLECT_SQL_ENV || COLLECT_SQL_PROP;
        }
    }
}
