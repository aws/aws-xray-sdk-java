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

package com.amazonaws.xray.entities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SearchPatternTest {

    @Test
    public void testInvalidArgs() {
        assertFalse(SearchPattern.wildcardMatch(null, ""));
        assertFalse(SearchPattern.wildcardMatch("", null));
        assertFalse(SearchPattern.wildcardMatch("", "whatever"));
    }

    @Test
    public void testMatchExactPositive() throws Exception {
        final String pat = "foo";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testMatchExactNegative() throws Exception {
        final String pat = "foo";
        final String str = "bar";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testSingleWildcardPositive() throws Exception {
        final String pat = "fo?";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testSingleWildcardNegative() throws Exception {
        final String pat = "f?o";
        final String str = "boo";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testMultipleWildcardPositive() throws Exception {
        final String pat = "?o?";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testMultipleWildcardNegative() throws Exception {
        final String pat = "f??";
        final String str = "boo";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobPositive() throws Exception {
        final String pat = "*oo";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobPositiveZeroOrMore() throws Exception {
        final String pat = "foo*";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobNegativeZeroOrMore() throws Exception {
        final String pat = "foo*";
        final String str = "fo0";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobNegative() throws Exception {
        final String pat = "fo*";
        final String str = "boo";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobAndSinglePositive() throws Exception {
        final String pat = "*o?";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testGlobAndSingleNegative() throws Exception {
        final String pat = "f?*";
        final String str = "boo";
        assertFalse(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void testPureWildcard() throws Exception {
        final String pat = "*";
        final String str = "foo";
        assertTrue(SearchPattern.wildcardMatch(pat, str));
    }

    @Test
    public void exactMatch() throws Exception {
        assertTrue(SearchPattern.wildcardMatch("6543210", "6543210"));
    }

    @Test
    public void testMisc() throws Exception {
        final String animal1 = "?at";
        final String animal2 = "?o?se";
        final String animal3 = "*s";

        final String vehicle1 = "J*";
        final String vehicle2 = "????";

        assertTrue(SearchPattern.wildcardMatch(animal1, "bat"));
        assertTrue(SearchPattern.wildcardMatch(animal1, "cat"));
        assertTrue(SearchPattern.wildcardMatch(animal2, "horse"));
        assertTrue(SearchPattern.wildcardMatch(animal2, "mouse"));
        assertTrue(SearchPattern.wildcardMatch(animal3, "dogs"));
        assertTrue(SearchPattern.wildcardMatch(animal3, "horses"));

        assertTrue(SearchPattern.wildcardMatch(vehicle1, "Jeep"));
        assertTrue(SearchPattern.wildcardMatch(vehicle2, "ford"));
        assertFalse(SearchPattern.wildcardMatch(vehicle2, "chevy"));
        assertTrue(SearchPattern.wildcardMatch("*", "cAr"));

        assertTrue(SearchPattern.wildcardMatch("*/foo", "/bar/foo"));
    }

    @Test
    public void testCaseInsensitivity() throws Exception {
        assertTrue(SearchPattern.wildcardMatch("Foo", "Foo", false));
        assertTrue(SearchPattern.wildcardMatch("Foo", "Foo", true));

        assertFalse(SearchPattern.wildcardMatch("Foo", "FOO", false));
        assertTrue(SearchPattern.wildcardMatch("Foo", "FOO", true));

        assertTrue(SearchPattern.wildcardMatch("Fo*", "Foo0", false));
        assertTrue(SearchPattern.wildcardMatch("Fo*", "Foo0", true));

        assertFalse(SearchPattern.wildcardMatch("Fo*", "FOo0", false));
        assertTrue(SearchPattern.wildcardMatch("Fo*", "FOO0", true));

        assertTrue(SearchPattern.wildcardMatch("Fo?", "Foo", false));
        assertTrue(SearchPattern.wildcardMatch("Fo?", "Foo", true));

        assertFalse(SearchPattern.wildcardMatch("Fo?", "FOo", false));
        assertTrue(SearchPattern.wildcardMatch("Fo?", "FoO", false));
        assertTrue(SearchPattern.wildcardMatch("Fo?", "FOO", true));
    }

    @Test
    public void testLongStrings() throws Exception {
        // This blew out the stack on a recursive version of wildcardMatch
        final char[] t = new char[] { 'a', 'b', 'c', 'd' };
        StringBuffer text = new StringBuffer("a");
        Random r = new Random();
        int size = 8192;

        for (int i = 0; i < size; i++) {
            text.append(t[Math.abs(r.nextInt()) % t.length]);
        }
        text.append("b");

        assertTrue(SearchPattern.wildcardMatch("a*b", text.toString()));
    }

    @Test
    public void testNoGlobs() throws Exception {
        assertFalse(SearchPattern.wildcardMatch("abcd", "abc"));
    }

    @Test
    public void testEdgeCaseGlobs() throws Exception {
        assertTrue(SearchPattern.wildcardMatch("", ""));
        assertTrue(SearchPattern.wildcardMatch("a", "a"));
        assertTrue(SearchPattern.wildcardMatch("*a", "a"));
        assertTrue(SearchPattern.wildcardMatch("*a", "ba"));
        assertTrue(SearchPattern.wildcardMatch("a*", "a"));
        assertTrue(SearchPattern.wildcardMatch("a*", "ab"));
        assertTrue(SearchPattern.wildcardMatch("a*a", "aa"));
        assertTrue(SearchPattern.wildcardMatch("a*a", "aba"));
        assertTrue(SearchPattern.wildcardMatch("a*a", "aaa"));
        assertTrue(SearchPattern.wildcardMatch("a*a*", "aa"));
        assertTrue(SearchPattern.wildcardMatch("a*a*", "aba"));
        assertTrue(SearchPattern.wildcardMatch("a*a*", "aaa"));
        assertTrue(SearchPattern.wildcardMatch("a*a*", "aaaaaaaaaaaaaaaaaaaaaaa"));
        assertTrue(SearchPattern.wildcardMatch("a*b*a*b*a*b*a*b*a*",
                "akljd9gsdfbkjhaabajkhbbyiaahkjbjhbuykjakjhabkjhbabjhkaabbabbaaakljdfsjklababkjbsdabab"));
        assertFalse(SearchPattern.wildcardMatch("a*na*ha", "anananahahanahana"));
    }

    @Test
    public void testMultiGlobs() throws Exception {

        // Technically, '**' isn't well defined Balsa, but the wildcardMatch should do the right thing with it.
        assertTrue(SearchPattern.wildcardMatch("*a", "a"));
        assertTrue(SearchPattern.wildcardMatch("**a", "a"));
        assertTrue(SearchPattern.wildcardMatch("***a", "a"));
        assertTrue(SearchPattern.wildcardMatch("**a*", "a"));
        assertTrue(SearchPattern.wildcardMatch("**a**", "a"));

        assertTrue(SearchPattern.wildcardMatch("a**b", "ab"));
        assertTrue(SearchPattern.wildcardMatch("a**b", "abb"));

        assertTrue(SearchPattern.wildcardMatch("*?", "a"));
        assertTrue(SearchPattern.wildcardMatch("*?", "aa"));
        assertTrue(SearchPattern.wildcardMatch("*??", "aa"));
        assertFalse(SearchPattern.wildcardMatch("*???", "aa"));
        assertTrue(SearchPattern.wildcardMatch("*?", "aaa"));

        assertTrue(SearchPattern.wildcardMatch("?", "a"));
        assertFalse(SearchPattern.wildcardMatch("??", "a"));

        assertTrue(SearchPattern.wildcardMatch("?*", "a"));
        assertTrue(SearchPattern.wildcardMatch("*?", "a"));
        assertFalse(SearchPattern.wildcardMatch("?*?", "a"));
        assertTrue(SearchPattern.wildcardMatch("?*?", "aa"));
        assertTrue(SearchPattern.wildcardMatch("*?*", "a"));

        assertFalse(SearchPattern.wildcardMatch("*?*a", "a"));
        assertTrue(SearchPattern.wildcardMatch("*?*a*", "ba"));
    }
}
