package com.amazonaws.xray.sql.mysql;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SanitizeTest {

    public String[] queries = {
        "a",
        "test 'asdf'",
        "test 'aaaaaaaa''aaaaaa'",
        "test 'aasdfasd' 'a' 'b'",
        "test ''",
        "test 'zz''''zz' ooo'aaa''aa'",
        "test 544 3456 43 '43' 345f",
        "test \"32f\" '32' 32",
        "test 'abcd'''",
        "now 'aabbccdd'''",
        "now 'aabbccdd\'\''",
        "now 'aabbccdd\'\''''",
        "now 'aabbccdd\'\''''",
        "now 'aabbccdd''\\''''"
    };

    public String[] sanitized = {
        "a",
        "test ?",
        "test ?",
        "test ? ? ?",
        "test ?",
        "test ? ooo?",
        "test ? ? ? ? 345f",
        "test ? ? ?",
        "test ?",
        "now ?",
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
