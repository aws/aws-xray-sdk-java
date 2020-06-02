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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AWSLogReferenceTest {

    AWSLogReference referenceA;
    AWSLogReference referenceB;
    AWSLogReference differentArn;
    AWSLogReference differentGroup;

    @Before
    public void setup() {
        referenceA = new AWSLogReference();
        referenceB = new AWSLogReference();
        differentArn = new AWSLogReference();
        differentGroup = new AWSLogReference();

        referenceA.setLogGroup("TEST");
        referenceA.setArn("arn:aws:test");

        referenceB.setLogGroup("TEST");
        referenceB.setArn("arn:aws:test");

        differentArn.setLogGroup("TEST");
        differentArn.setArn("arn:aws:nottest");

        differentGroup.setLogGroup("NOTTEST");
        differentGroup.setArn("arn:aws:test");
    }

    // Test case for equals.
    @SuppressWarnings("SelfEquals")
    @Test
    public void testEqualityPositive() {
        assertTrue(referenceA.equals(referenceB));
        assertTrue(referenceB.equals(referenceA));
        assertTrue(referenceA.equals(referenceA));
    }

    @Test
    public void testEqualityNegativeBecauseArn() {
        assertEquals(false, referenceA.equals(differentArn));
        assertEquals(false, differentArn.equals(referenceA));
    }

    @Test
    public void testEqualityNegativeBecauseGroup() {
        assertEquals(false, referenceA.equals(differentGroup));
        assertEquals(false, differentGroup.equals(referenceA));
    }
}
