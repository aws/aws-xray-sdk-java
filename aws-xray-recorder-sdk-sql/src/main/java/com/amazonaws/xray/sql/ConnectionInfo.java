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

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

final class ConnectionInfo {
    @Nullable
    private final String sanitizedUrl;
    @Nullable
    private final String user;
    @Nullable
    private final String host;
    @Nullable
    private final String dbName;

    private ConnectionInfo(@Nullable String sanitizedUrl,
                           @Nullable String user,
                           @Nullable String host,
                           @Nullable String dbName) {
        this.sanitizedUrl = sanitizedUrl;
        this.user = user;
        this.host = host;
        this.dbName = dbName;
    }

    String getSanitizedUrl() {
        return sanitizedUrl;
    }

    String getUser() {
        return user;
    }

    String getHost() {
        return host;
    }

    String getDbName() {
        return dbName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionInfo that = (ConnectionInfo) o;
        return Objects.equals(sanitizedUrl, that.sanitizedUrl) &&
            Objects.equals(user, that.user) &&
            Objects.equals(host, that.host) &&
            Objects.equals(dbName, that.dbName);
    }

    @Override
    public int hashCode() {
        int result = sanitizedUrl != null ? sanitizedUrl.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (dbName != null ? dbName.hashCode() : 0);
        return result;
    }

    static class Builder {
        private String sanitizedUrl;
        private String user;
        private String host;
        private String dbName;

        Builder sanitizedUrl(String sanitizedUrl) {
            this.sanitizedUrl = sanitizedUrl;
            return this;
        }

        Builder user(String user) {
            this.user = user;
            return this;
        }

        Builder host(String host) {
            this.host = host;
            return this;
        }

        Builder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        ConnectionInfo build() {
            return new ConnectionInfo(sanitizedUrl, user, host, dbName);
        }
    }
}
