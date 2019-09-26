package com.amazonaws.xray.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

class TracingConnection {
    static Connection decorate(Connection c) {
        return (Connection) Proxy.newProxyInstance(TracingConnection.class.getClassLoader(),
                new Class[] { Connection.class },
                new TracingConnectionHandler(c));
    }

    private static class TracingConnectionHandler implements InvocationHandler {

        private final Connection original;

        TracingConnectionHandler(Connection original) {
            this.original = original;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws IllegalAccessException, IllegalArgumentException,
                InvocationTargetException {
            //intercepted methods taken from https://github.com/aws/aws-xray-sdk-java/blob/master/aws-xray-recorder-sdk-sql-mysql/src/main/java/com/amazonaws/xray/sql/mysql/TracingInterceptor.java
            if (method.getName().equals("prepareCall") || method.getName().equals("prepareStatement") || method.getName().equals("createStatement")) {
                Statement s = (Statement) method.invoke(original, args);
                return TracingStatement.decorate(s, method.getName(), args);
            }
            //else, simply delegates
            return method.invoke(original, args);
        }
    }
}
