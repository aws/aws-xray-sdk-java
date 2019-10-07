package com.amazonaws.xray.sql;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class TracingDataSource {

    /**
     * Call this method on your {@link DataSource} before any calls to #getConnection in order to have all your SQL
     * queries added in a X-Ray Subsegment.
     * If X-Ray is incorrectly configured, all queries will be traced to stdout.
     * If there is a problem to retrieve DB metadata, the X-Ray trace will contain "strange SQLException occurred ...".
     *
     * @param ds the datasource to decorate
     * @return a DataSource that traces all SQL queries in X-Ray
     */
    public static DataSource decorate(DataSource ds) {
        return (DataSource) Proxy.newProxyInstance(TracingDataSource.class.getClassLoader(),
                new Class[] { DataSource.class },
                new TracingDatasourceHandler(ds));
    }

    private static class TracingDatasourceHandler implements InvocationHandler {

        private final DataSource original;

        TracingDatasourceHandler(DataSource original) {
            this.original = original;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws IllegalAccessException, IllegalArgumentException,
                InvocationTargetException {
            if (isGetConnection(method)) {
                Connection con = (Connection) method.invoke(original, args);
                return TracingConnection.decorate(con);
            }
            //else, simply delegates
            return method.invoke(original, args);
        }

        private static final String GET_CONNECTION = "getConnection";

        private boolean isGetConnection(Method method) {
            return method.getName().equals(GET_CONNECTION);
        }
    }
}