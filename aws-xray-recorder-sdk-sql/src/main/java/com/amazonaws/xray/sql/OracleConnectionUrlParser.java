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

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.xray.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for parsing Oracle database connection URLs and extracting useful metadata.
 * Adapted from the OpenTelemetry JdbcConnectionUrlParser class under the Apache 2.0 license.
 *
 * Original source: https://bit.ly/2OZB5jR
 * Oracle URL documentation: https://docs.oracle.com/cd/B28359_01/java.111/b31224/urls.htm
 */
final class OracleConnectionUrlParser {
    private static final Log log = LogFactory.getLog(OracleConnectionUrlParser.class);

    private static final Pattern HOST_PATTERN = Pattern.compile("\\(\\s*host\\s*=\\s*([^ )]+)\\s*\\)");
    private static final Pattern INSTANCE_PATTERN = Pattern.compile("\\(\\s*service_name\\s*=\\s*([^ )]+)\\s*\\)");


    static ConnectionInfo parseUrl(String jdbcUrl, ConnectionInfo.Builder builder) {
        jdbcUrl = jdbcUrl.toLowerCase();
        recordSanitizedUrl(jdbcUrl, builder);
        int subtypeEndIndex = jdbcUrl.indexOf(":", "jdbc:oracle:".length());
        if (subtypeEndIndex < 0) {
            return builder.build();
        }

        // Strip the constant prefix for simplicity
        jdbcUrl = jdbcUrl.substring(subtypeEndIndex + 1);

        if (jdbcUrl.contains("@")) {
            return parseUrlWithAt(jdbcUrl, builder).build();
        } else {
            return parseOracleConnectInfo(jdbcUrl, builder).build();
        }
    }

    private static ConnectionInfo.Builder recordSanitizedUrl(String jdbcUrl, ConnectionInfo.Builder builder) {
        int atLoc = jdbcUrl.indexOf("@");
        int userEndLoc = jdbcUrl.indexOf("/");
        if (userEndLoc != -1 && userEndLoc < atLoc) {
            builder.sanitizedUrl(jdbcUrl.substring(0, userEndLoc) + jdbcUrl.substring(atLoc));
        } else {
            builder.sanitizedUrl(jdbcUrl);
        }

        return builder;
    }

    private static ConnectionInfo.Builder parseOracleConnectInfo(String jdbcUrl, ConnectionInfo.Builder builder) {
        String host;
        String instance;

        int hostEnd = jdbcUrl.indexOf(":");
        int instanceLoc = jdbcUrl.indexOf("/");
        if (hostEnd > 0) {
            host = jdbcUrl.substring(0, hostEnd);
            int afterHostEnd = jdbcUrl.indexOf(":", hostEnd + 1);
            if (afterHostEnd > 0) {
                instance = jdbcUrl.substring(afterHostEnd + 1);
            } else {
                if (instanceLoc > 0) {
                    instance = jdbcUrl.substring(instanceLoc + 1);
                } else {
                    String portOrInstance = jdbcUrl.substring(hostEnd + 1);
                    Integer parsedPort = null;
                    try {
                        parsedPort = Integer.parseInt(portOrInstance);
                    } catch (NumberFormatException e) {
                        log.debug(e.getMessage(), e);
                    }
                    if (parsedPort == null) {
                        instance = portOrInstance;
                    } else {
                        instance = null;
                    }
                }
            }
        } else {
            if (instanceLoc > 0) {
                host = jdbcUrl.substring(0, instanceLoc);
                instance = jdbcUrl.substring(instanceLoc + 1);
            } else {
                if (jdbcUrl.isEmpty()) {
                    return builder;
                } else {
                    host = null;
                    instance = jdbcUrl;
                }
            }
        }
        if (host != null) {
            builder.host(host);
        }
        return builder.dbName(instance);
    }

    private static ConnectionInfo.Builder parseUrlWithAt(String jdbcUrl, ConnectionInfo.Builder builder) {
        if (jdbcUrl.contains("@(description")) {
            return parseDescription(jdbcUrl, builder);
        }
        String user;

        String[] atSplit = jdbcUrl.split("@", 2);

        int userInfoLoc = atSplit[0].indexOf("/");
        if (userInfoLoc > 0) {
            user = atSplit[0].substring(0, userInfoLoc);
        } else {
            user = null;
        }

        String connectInfo = atSplit[1];
        int hostStart;
        if (connectInfo.startsWith("//")) {
            hostStart = "//".length();
        } else if (connectInfo.startsWith("ldap://")) {
            hostStart = "ldap://".length();
        } else {
            hostStart = 0;
        }
        if (user != null) {
            builder.user(user);
        }
        return parseOracleConnectInfo(connectInfo.substring(hostStart), builder);
    }

    private static ConnectionInfo.Builder parseDescription(String jdbcUrl, ConnectionInfo.Builder builder) {
        String[] atSplit = jdbcUrl.split("@", 2);

        int userInfoLoc = atSplit[0].indexOf("/");
        if (userInfoLoc > 0) {
            builder.user(atSplit[0].substring(0, userInfoLoc));
        }

        Matcher hostMatcher = HOST_PATTERN.matcher(atSplit[1]);
        if (hostMatcher.find()) {
            builder.host(hostMatcher.group(1));
        }

        Matcher instanceMatcher = INSTANCE_PATTERN.matcher(atSplit[1]);
        if (instanceMatcher.find()) {
            builder.dbName(instanceMatcher.group(1));
        }

        return builder;
    }


    private OracleConnectionUrlParser() {
    }
}
