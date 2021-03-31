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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test class for parsing a bunch of acceptable Oracle URLs. The test cases are taken from the
 * OpenTelemetry JdbcConnectionUrlParser test class.
 *
 * Original source: https://bit.ly/3mhFCud
 */
class OracleConnectionUrlParserTest {

    // https://docs.oracle.com/cd/B28359_01/java.111/b31224/urls.htm
    // https://docs.oracle.com/cd/B28359_01/java.111/b31224/jdbcthin.htm
    // https://docs.oracle.com/cd/B28359_01/java.111/b31224/instclnt.htm
    private static final String[] ORACLE_URLS = {
        "jdbc:oracle:thin:orcluser/PW@localhost:55:orclsn",
        "jdbc:oracle:thin:orcluser/PW@//orcl.host:55/orclsn",
        "jdbc:oracle:thin:orcluser/PW@127.0.0.1:orclsn",
        "jdbc:oracle:thin:orcluser/PW@//orcl.host/orclsn",
        "jdbc:oracle:thin:@//orcl.host:55/orclsn",
        "jdbc:oracle:thin:@ldap://orcl.host:55/some,cn=OracleContext,dc=com",
        "jdbc:oracle:thin:127.0.0.1:orclsn",
        "jdbc:oracle:thin:orcl.host:orclsn",
        "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST= 127.0.0.1 )(POR T= 666))" +
            "(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=orclsn)))",
        "jdbc:oracle:drivertype:orcluser/PW@orcl.host:55/orclsn",
        "jdbc:oracle:oci8:@",
        "jdbc:oracle:oci8:@orclsn",
        "jdbc:oracle:oci:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)( HOST =  orcl.host )" +
            "( PORT = 55  ))(CONNECT_DATA=(SERVICE_NAME =orclsn  )))"
    };

    private static final ConnectionInfo[] EXPECTED_INFO = {
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:orcluser@localhost:55:orclsn").user("orcluser")
            .host("localhost").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:orcluser@//orcl.host:55/orclsn").user("orcluser")
            .host("orcl.host").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:orcluser@127.0.0.1:orclsn").user("orcluser")
            .host("127.0.0.1").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:orcluser@//orcl.host/orclsn").user("orcluser")
            .host("orcl.host").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:@//orcl.host:55/orclsn")
            .host("orcl.host").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:@ldap://orcl.host:55/some,cn=oraclecontext,dc=com")
            .host("orcl.host").dbName("some,cn=oraclecontext,dc=com").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:127.0.0.1:orclsn")
            .host("127.0.0.1").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:thin:orcl.host:orclsn")
            .host("orcl.host").dbName("orclsn").build(),
        new ConnectionInfo.Builder()
            .sanitizedUrl("jdbc:oracle:thin:@(description=(address=(protocol=tcp)(host= 127.0.0.1 )(por t= 666))" +
            "(connect_data=(server=dedicated)(service_name=orclsn)))").host("127.0.0.1").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:drivertype:orcluser@orcl.host:55/orclsn").user("orcluser")
            .host("orcl.host").dbName("orclsn").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:oci8:@").build(),
        new ConnectionInfo.Builder().sanitizedUrl("jdbc:oracle:oci8:@orclsn").dbName("orclsn").build(),
        new ConnectionInfo.Builder()
            .sanitizedUrl("jdbc:oracle:oci:@(description=(address=(protocol=tcp)( host =  orcl.host )( port = 55  ))" +
            "(connect_data=(service_name =orclsn  )))").host("orcl.host").dbName("orclsn").build()
    };

    @Test
    void testUrlParsing() {
        assertThat(ORACLE_URLS.length).isEqualTo(EXPECTED_INFO.length);
        for (int i = 0; i < ORACLE_URLS.length; i++) {
            ConnectionInfo.Builder builder = new ConnectionInfo.Builder();
            ConnectionInfo parsed = OracleConnectionUrlParser.parseUrl(ORACLE_URLS[i], builder);
            assertThat(parsed).isEqualTo(EXPECTED_INFO[i]);
        }
    }
}
