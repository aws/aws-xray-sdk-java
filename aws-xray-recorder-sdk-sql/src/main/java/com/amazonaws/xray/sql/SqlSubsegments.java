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
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class containing utility method to create fully-populated SQL subsegments.
 * See https://docs.aws.amazon.com/xray/latest/devguide/xray-api-segmentdocuments.html#api-segmentdocuments-sql
 */
public final class SqlSubsegments {
    private static final Log logger = LogFactory.getLog(SqlSubsegments.class);

    private static Map<Connection, ConnectionInfo> connMap = new WeakHashMap<>();

    /**
     * The URL of the database this query is made on
     */
    public static final String URL = "url";

    /**
     * The database username
     */
    public static final String USER = "user";

    /**
     * The version of the database driver library used for this database
     */
    public static final String DRIVER_VERSION = "driver_version";

    /**
     * The type of SQL Database this query is done on, like MySQL or HikariCP
     */
    public static final String DATABASE_TYPE = "database_type";

    /**
     * The version of the database product itself, like MySQL 8.0
     */
    public static final String DATABASE_VERSION = "database_version";

    /**
     * The SQL query string used in this query. This is not recorded in subsegments by default due to security issues.
     * SDK users may use this key or {@link #forQuery} to manually record their queries if they wish.
     * See https://github.com/aws/aws-xray-sdk-java/issues/28
     */
    public static final String SANITIZED_QUERY = "sanitized_query";

    /**
     * The fallback name for subsegments representing SQL queries that failed to be named dynamically
     */
    public static final String DEFAULT_DATABASE_NAME = "database";

    /**
     * Begins a {@link Subsegment} populated with data provided by the {@link Connection#getMetaData} method. Includes
     * the SQL query string if it is non-null, omits it otherwise. Takes care to swallow any potential
     * {@link SQLException}s and always start a subsegment for consistency.
     *
     * @param connection the JDBC connection object used for the query this {@link Subsegment} represents.
     * @param query the SQL query string used in this query, or {@code null} if it is not desirable to include in the
     *              subsegment, e.g. for security concerns.
     * @return the created {@link Subsegment}.
     */
    public static Subsegment forQuery(Connection connection, @Nullable String query) {
        logger.info("In forQuery with connection: " + connection + " and query: " + query);
        DatabaseMetaData metadata;
        ConnectionInfo connectionInfo = connMap.get(connection);
        String subsegmentName = DEFAULT_DATABASE_NAME;

        try {
            metadata = connection.getMetaData();
            String connUrl = metadata.getURL();
            logger.info("Got connUrl: " + connUrl);
            if (connectionInfo != null) {
                logger.info("cache hit!!\n" + connectionInfo.toString());
            }

            // Parse URL if Oracle
            if (connectionInfo == null && connUrl != null && connUrl.contains("oracle")) {
                connectionInfo = OracleConnectionUrlParser.parseUrl(connUrl, new ConnectionInfo.Builder());
                connMap.put(connection, connectionInfo);
            } else if (connectionInfo == null) {
                connectionInfo = new ConnectionInfo.Builder().build();
            }

            // Get database name if available; otherwise fallback to default
            String database;
            if (connectionInfo.getDbName() != null) {
                database = connectionInfo.getDbName();
            } else if (connection.getCatalog() != null) {
                database = connection.getCatalog();
            } else {
                database = DEFAULT_DATABASE_NAME;
            }

            logger.info("determined database name: " + database);

            // Get database host if available; otherwise omit host
            String host = null;
            if (connectionInfo.getHost() != null) {
                host = connectionInfo.getHost();
            } else {
                try {
                    host = new URI(new URI(metadata.getURL()).getSchemeSpecificPart()).getHost();
                } catch (URISyntaxException e) {
                    logger.debug("Unable to parse database URI. Falling back to default '" + DEFAULT_DATABASE_NAME
                        + "' for subsegment name.", e);
                }
            }

            logger.info("determined host: " + host);

            // Fully formed subsegment name is of form "dbName@host"
            subsegmentName = database + (host != null ? "@" + host : "");
        } catch (SQLException e) {
            logger.debug("Encountered exception while retrieving metadata for SQL subsegment "
                + ", starting blank subsegment instead");
            return AWSXRay.beginSubsegment(subsegmentName);
        }

        Subsegment subsegment = AWSXRay.beginSubsegment(subsegmentName);
        subsegment.setNamespace(Namespace.REMOTE.toString());

        try {
            subsegment.putSql(URL, connectionInfo.getSanitizedUrl() != null ?
                connectionInfo.getSanitizedUrl() : metadata.getURL());
            subsegment.putSql(USER, connectionInfo.getUser() != null ? connectionInfo.getUser() : metadata.getUserName());
            subsegment.putSql(DRIVER_VERSION, metadata.getDriverVersion());
            subsegment.putSql(DATABASE_TYPE, metadata.getDatabaseProductName());
            subsegment.putSql(DATABASE_VERSION, metadata.getDatabaseProductVersion());
        } catch (SQLException e) {
            logger.debug("Encountered exception while populating SQL subsegment metadata", e);
        }

        if (query != null) {
            subsegment.putSql(SANITIZED_QUERY, query);
        }

        return subsegment;
    }

    // Visible for testing
    static void setConnMap(Map connMap) {
        SqlSubsegments.connMap = connMap;
    }

    private SqlSubsegments() {
    }
}
