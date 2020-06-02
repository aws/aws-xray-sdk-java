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

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SanitizeTest {

    public String[] queries = {
        "a",
        "test 'asdf'",
        "test $$asdf$$",
        "test $$zxcv'jdfa'dzxc$$",
        "test $a$bbbbb$a$",                     //5
        "test $a$bbb$zzz$b$aaaaaaaaa$b$bbb$a$",
        "test $a$wwww$a$ test $b$www$b$",
        "test 'aaaaaaaa''aaaaaa'",
        "test 'aasdfasd' $$b$$ $a$a$a$",
        "test 'zz' $$b$$ $a$a$a$ 'zz'",         //10
        "test ''",
        "test 'zz''''zz' ooo'aaa''aa'",
        "test 544 3456 43 '43' 345f",
        "test \"32f\" '32' 32",
        "test $aaa$wwww$aaa$ test",
        "test $aaa$wwww$aaa$ $1 this is a test",
        "now 'aabbccdd'''",
        "now 'aabbccdd\\'\\''",
        "now 'aabbccdd\\'\\''''",
        "now 'aabbccdd''\\''''"
    };

    public String[] sanitized = {
        "a",
        "test ?",
        "test ?",
        "test ?",
        "test ?",                               //5
        "test ?",
        "test ? test ?",
        "test ?",
        "test ? ? ?",
        "test ? ? ? ?",                         //10
        "test ?",
        "test ? ooo?",
        "test ? ? ? ? 345f",
        "test \"32f\" ? ?",
        "test ? test",
        "test ? $? this is a test",
        "now ?",
        "now ?",
        "now ?",
        "now ?"
    };

    /*@Test
    public void testSanitizeQuery() {
        for (int i = 0; i < queries.length; i++) {
            Assert.assertEquals("Sanitizing: " + queries[i], sanitized[i], TracingInterceptor.sanitizeSql(queries[i]));
        }
    }*/

    /*@Test
    public void testVeryLongString() {
        String delimiter = "$a$";
        StringBuilder mega = new StringBuilder();
        for (int i = 0; i < 1000000; i++) { // ten million copies
            mega.append("Fkwekfjb2k3hf4@#$'fbb4kbf'$4'fbf''4$'");
        }
        String veryLong = "test " + delimiter + mega.toString() + delimiter;
        Assert.assertEquals("test ?", TracingInterceptor.sanitizeSql(veryLong));
    }*/
}
