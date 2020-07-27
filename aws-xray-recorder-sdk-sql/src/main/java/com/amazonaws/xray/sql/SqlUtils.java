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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class containing utility methods used by X-Ray's SQL tracing libraries.
 */
public final class SqlUtils {
    private static final Log logger = LogFactory.getLog(SqlUtils.class);

    // https://docs.aws.amazon.com/xray/latest/devguide/xray-api-segmentdocuments.html#api-segmentdocuments-sql
    private static final String DEFAULT_DATABASE_NAME = "database";
    private static final String URL = "url";
    private static final String USER = "user";
    private static final String DRIVER_VERSION = "driver_version";
    private static final String DATABASE_TYPE = "database_type";
    private static final String DATABASE_VERSION = "database_version";
    private static final String SANITIZED_QUERY = "sanitized_query";

    private SqlUtils() {
    }

    /**
     * Begins a {@link Subsegment} populated with data provided by the {@link Connection#getMetaData} method. Includes
     * the SQL query string if it is non-null, omits it otherwise.
     *
     * @param connection the JDBC connection object used for the query this {@link Subsegment} represents.
     * @param query the SQL query string used in this query, or {@code null} if it is not desirable to include in the
     *              subsegment, e.g. for security concerns.
     * @return the created {@link Subsegment}.
     * @throws SQLException if there is a bad interaction with the {@link Connection}.
     */
    @Nullable
    public static Subsegment createSqlSubsegment(Connection connection, @Nullable String query) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String subsegmentName = DEFAULT_DATABASE_NAME;
        try {
            URI normalizedUri = new URI(new URI(metadata.getURL()).getSchemeSpecificPart());
            subsegmentName = connection.getCatalog() + "@" + normalizedUri.getHost();
        } catch (URISyntaxException e) {
            logger.warn("Unable to parse database URI. Falling back to default '" + DEFAULT_DATABASE_NAME
                    + "' for subsegment name.", e);
        }

        Subsegment subsegment = AWSXRay.beginSubsegment(subsegmentName);
        if (subsegment == null) {
            return null;
        }

        subsegment.setNamespace(Namespace.REMOTE.toString());
        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put(URL, metadata.getURL());
        sqlParams.put(USER, metadata.getUserName());
        sqlParams.put(DRIVER_VERSION, metadata.getDriverVersion());
        sqlParams.put(DATABASE_TYPE, metadata.getDatabaseProductName());
        sqlParams.put(DATABASE_VERSION, metadata.getDatabaseProductVersion());

        if (query != null) {
            sqlParams.put(SANITIZED_QUERY, query);
        }

        subsegment.putAllSql(sqlParams);
        return subsegment;
    }
}
