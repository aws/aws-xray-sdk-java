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

package com.amazonaws.xray.sql.postgres;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import org.apache.tomcat.jdbc.pool.PooledConnection;

/*
 * Inspired by: http://grepcode.com/file/repo1.maven.org/maven2/org.apache.tomcat/tomcat-jdbc/8.0.24/org/apache/tomcat/jdbc/pool/interceptor/AbstractQueryReport.java#AbstractQueryReport
 */
public class TracingInterceptor extends JdbcInterceptor {

    protected static final String CREATE_STATEMENT = "createStatement";
    protected static final int CREATE_STATEMENT_INDEX = 0;
    protected static final String PREPARE_STATEMENT = "prepareStatement";
    protected static final int PREPARE_STATEMENT_INDEX = 1;
    protected static final String PREPARE_CALL = "prepareCall";
    protected static final int PREPARE_CALL_INDEX = 2;

    protected static final String[] STATEMENT_TYPES = {CREATE_STATEMENT, PREPARE_STATEMENT, PREPARE_CALL};
    protected static final int STATEMENT_TYPE_COUNT = STATEMENT_TYPES.length;

    protected static final String EXECUTE = "execute";
    protected static final String EXECUTE_QUERY = "executeQuery";
    protected static final String EXECUTE_UPDATE = "executeUpdate";
    protected static final String EXECUTE_BATCH = "executeBatch";

    protected static final String[] EXECUTE_TYPES = {EXECUTE, EXECUTE_QUERY, EXECUTE_UPDATE, EXECUTE_BATCH};

    /**
     * @deprecated For internal use only.
     */
    @SuppressWarnings("checkstyle:ConstantName")
    @Deprecated
    protected static final Constructor<?>[] constructors =
        new Constructor[STATEMENT_TYPE_COUNT];

    private static final Log logger =
        LogFactory.getLog(TracingInterceptor.class);

    private static final String DEFAULT_DATABASE_NAME = "database";

    /**
     * Creates a constructor for a proxy class, if one doesn't already exist
     *
     * @param index the index of the constructor
     * @param clazz the interface that the proxy will implement
     * @return returns a constructor used to create new instances
     * @throws NoSuchMethodException
     */
    protected Constructor<?> getConstructor(int index, Class<?> clazz) throws NoSuchMethodException {
        if (constructors[index] == null) {
            Class<?> proxyClass = Proxy.getProxyClass(TracingInterceptor.class.getClassLoader(), new Class[] {clazz});
            constructors[index] = proxyClass.getConstructor(new Class[] {InvocationHandler.class});
        }
        return constructors[index];
    }

    public Object createStatement(Object proxy, Method method, Object[] args, Object statementObject) {
        try {
            String name = method.getName();
            String sql = null;
            Constructor<?> constructor = null;
            Map<String, Object> additionalParams = new HashMap<>();
            if (compare(CREATE_STATEMENT, name)) {
                //createStatement
                constructor = getConstructor(CREATE_STATEMENT_INDEX, Statement.class);
            } else if (compare(PREPARE_STATEMENT, name)) {
                additionalParams.put("preparation", "statement");
                sql = (String) args[0];
                constructor = getConstructor(PREPARE_STATEMENT_INDEX, PreparedStatement.class);
            } else if (compare(PREPARE_CALL, name)) {
                additionalParams.put("preparation", "call");
                sql = (String) args[0];
                constructor = getConstructor(PREPARE_CALL_INDEX, CallableStatement.class);
            } else {
                //do nothing, might be a future unsupported method
                //so we better bail out and let the system continue
                return statementObject;
            }
            Statement statement = ((Statement) statementObject);
            Connection connection = statement.getConnection();
            DatabaseMetaData metadata = connection.getMetaData();
            // parse cname for subsegment name
            additionalParams.put("url", metadata.getURL());
            additionalParams.put("user", metadata.getUserName());
            additionalParams.put("driver_version", metadata.getDriverVersion());
            additionalParams.put("database_type", metadata.getDatabaseProductName());
            additionalParams.put("database_version", metadata.getDatabaseProductVersion());
            String hostname = DEFAULT_DATABASE_NAME;
            try {
                URI normalizedUri = new URI(new URI(metadata.getURL()).getSchemeSpecificPart());
                hostname = connection.getCatalog() + "@" + normalizedUri.getHost();
            } catch (URISyntaxException e) {
                logger.warn("Unable to parse database URI. Falling back to default '" + DEFAULT_DATABASE_NAME
                            + "' for subsegment name.", e);
            }

            logger.debug("Instantiating new statement proxy.");
            return constructor.newInstance(new TracingStatementProxy(statementObject, sql, hostname, additionalParams));
        } catch (SQLException | InstantiationException | IllegalAccessException | InvocationTargetException
            | NoSuchMethodException e) {
            logger.warn("Unable to create statement proxy for tracing.", e);
        }
        return statementObject;
    }

    protected class TracingStatementProxy implements InvocationHandler {
        protected boolean closed = false;
        protected Object delegate;
        protected final String query;
        protected final String hostname;
        protected Map<String, Object> additionalParams;

        public TracingStatementProxy(Object parent, String query, String hostname, Map<String, Object> additionalParams) {
            this.delegate = parent;
            this.query = query;
            this.hostname = hostname;
            this.additionalParams = additionalParams;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //get the name of the method for comparison
            final String name = method.getName();
            //was close invoked?
            boolean close = compare(JdbcInterceptor.CLOSE_VAL, name);
            //allow close to be called multiple times
            if (close && closed) {
                return null;
            }
            //are we calling isClosed?
            if (compare(JdbcInterceptor.ISCLOSED_VAL, name)) {
                return Boolean.valueOf(closed);
            }
            //if we are calling anything else, bail out
            if (closed) {
                throw new SQLException("Statement closed.");
            }
            //check to see if we are about to execute a query
            final boolean process = isExecute(method);
            Object result = null;
            Subsegment subsegment = null;
            if (process) {
                subsegment = AWSXRay.beginSubsegment(hostname);
            }
            try {
                if (process && null != subsegment) {
                    subsegment.putAllSql(additionalParams);
                    subsegment.setNamespace(Namespace.REMOTE.toString());
                }
                result = method.invoke(delegate, args); //execute the query
            } catch (Throwable t) {
                if (null != subsegment) {
                    subsegment.addException(t);
                }
                if (t instanceof InvocationTargetException && t.getCause() != null) {
                    throw t.getCause();
                } else {
                    throw t;
                }
            } finally {
                if (process && null != subsegment) {
                    AWSXRay.endSubsegment();
                }
            }

            //perform close cleanup
            if (close) {
                closed = true;
                delegate = null;
            }

            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (compare(CLOSE_VAL, method)) {
            return super.invoke(proxy, method, args);
        } else {
            boolean process = isStatement(method);
            if (process) {
                Object statement = super.invoke(proxy, method, args);
                return createStatement(proxy, method, args, statement);
            } else {
                return super.invoke(proxy, method, args);
            }
        }
    }


    private boolean isStatement(Method method) {
        return isMemberOf(STATEMENT_TYPES, method);
    }

    private boolean isExecute(Method method) {
        return isMemberOf(EXECUTE_TYPES, method);
    }

    protected boolean isMemberOf(String[] names, Method method) {
        boolean member = false;
        final String name = method.getName();
        for (int i = 0; !member && i < names.length; i++) {
            member = compare(names[i], name);
        }
        return member;
    }

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        //do nothing
    }
}
