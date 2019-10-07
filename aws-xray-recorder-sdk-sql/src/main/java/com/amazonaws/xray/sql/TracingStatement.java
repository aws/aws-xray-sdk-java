package com.amazonaws.xray.sql;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

class TracingStatement {
    static Statement decorate(Statement s, String invokedMethodName, Object[] invokedMethodArgs) {
        return (Statement) Proxy.newProxyInstance(TracingConnection.class.getClassLoader(),
                new Class[] { resolveProxiedInterface(invokedMethodName) },
                new TracingStatementHandler(s, resolveSqlQuery(invokedMethodName, invokedMethodArgs)));
    }

    private static Class resolveProxiedInterface(String invokedMethodName) {
        switch (invokedMethodName) {
            case "prepareCall": return CallableStatement.class;
            case "prepareStatement": return PreparedStatement.class;
            case "createStatement": return Statement.class;
            default: throw new IllegalStateException("TracingSegment can not decorate " + invokedMethodName);
        }
    }

    private static String resolveSqlQuery(String invokedMethodName, Object[] invokedMethodArgs) {
        switch (invokedMethodName) {
            case "prepareCall":
            case "prepareStatement":
                return (String) invokedMethodArgs[0];
            case "createStatement":
                return null; //the correct sql will be retrieved later in resolveExecutedSql
            default: throw new IllegalStateException("TracingSegment can not resolve sql for " + invokedMethodName);
        }
    }

    private static class TracingStatementHandler implements InvocationHandler {

        private final Statement original;
        private final String sql;

        TracingStatementHandler(Statement original, String sql) {
            this.original = original;
            this.sql = sql;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //mostly based on https://github.com/aws/aws-xray-sdk-java/blob/master/aws-xray-recorder-sdk-sql-mysql/src/main/java/com/amazonaws/xray/sql/mysql/TracingInterceptor.java
            // but in a more straightforward logic (IMHO :) )
            if (method.getName().equals("execute") || method.getName().equals("executeQuery") || method.getName().equals("executeUpdate") || method.getName().equals("executeBatch")) {
                //invoke the original method "wrapped" in a XRay Subsegment
                Subsegment subsegment = AWSXRay.beginSubsegment("SQL");
                subsegment.putAllSql(extractSqlParams(method, args));
                subsegment.setNamespace(Namespace.REMOTE.toString());
                try {
                    return method.invoke(original, args); //execute the query
                } catch (Throwable t) {
                    subsegment.addException(t);
                    if (t instanceof InvocationTargetException && t.getCause() != null) {
                        throw t.getCause();
                    } else {
                        throw t;
                    }
                } finally {
                    AWSXRay.endSubsegment();
                }

            } else {
                //else, simply delegates
                return method.invoke(original, args);
            }
        }

        private Map<String, Object> extractSqlParams(Method method, Object[] args) {
            Map<String, Object> additionalParams = new HashMap<>();
            try {
                DatabaseMetaData metadata = original.getConnection().getMetaData();
                additionalParams.put("url", metadata.getURL());
                additionalParams.put("driver_version", metadata.getDriverVersion());
                additionalParams.put("database_type", metadata.getDatabaseProductName());
                additionalParams.put("database_version", metadata.getDatabaseProductVersion());
                additionalParams.put("sanitized_query", sanitizeSql(resolveExecutedSql(method.getName(), args)));
            } catch (SQLException e) {
                //a SqlException at this stage is kind of strange ... just ignore
                // (i.e the XRay trace will not contain the info but this is not very severe)
                //at least, we provide a way to identify the problem in the XRay trace
                additionalParams.put("url", "strange SQLException occurred ...");
            }
            return additionalParams;
        }

        private String resolveExecutedSql(String invokedMethodName, Object[] invokedMethodArgs) {
            switch (invokedMethodName) {
                case "execute":
                case "executeQuery":
                case "executeUpdate":
                    return invokedMethodArgs != null && invokedMethodArgs.length > 0 && (invokedMethodArgs[0] instanceof String) ? (String) invokedMethodArgs[0] : sql;
                case "executeBatch":
                    //retrieving the executed queries is possible ... but that would produce a very big output !
                    return "BATCH";
                default:
                    throw new IllegalStateException("can not resolve executed SQL for " + invokedMethodName);
            }
        }

        private String sanitizeSql(String sql) {
            //TODO: ask programmer consent and log intelligently
            //First problem : we should not expose sensitive data here, at least without the programmer consent
            //  note that in the .net SDK, they simply have a look at a config var (https://github.com/aws/aws-xray-sdk-dotnet/blob/master/sdk/src/Handlers/SqlServer/DbCommandInterceptor.cs > ShouldCollectSqlText)
            //  also note that in the python SDK, they simply trust SqlAlchemy or Django ...
            //Second problem : we should not log something too big (because that would incur storage costs all over AWS infrastructure)
            //  however, just truncating is a bit harsh ...
            return sql != null && sql.length() > 300 ? (sql.substring(0, 297)+"...") : sql;
        }
    }
}
