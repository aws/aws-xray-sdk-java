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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AWSLogReferenceTest {

    AWSLogReference referenceA;
    AWSLogReference referenceB;
    AWSLogReference differentArn;
    AWSLogReference differentGroup;

    @BeforeEach
    void setup() {
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
    void testEqualityPositive() {
        Assertions.assertTrue(referenceA.equals(referenceB));
        Assertions.assertTrue(referenceB.equals(referenceA));
        Assertions.assertTrue(referenceA.equals(referenceA));
    }

    @Test
    void testEqualityNegativeBecauseArn() {
        Assertions.assertEquals(false, referenceA.equals(differentArn));
        Assertions.assertEquals(false, differentArn.equals(referenceA));
    }

    @Test
    void testEqualityNegativeBecauseGroup() {
        Assertions.assertEquals(false, referenceA.equals(differentGroup));
        Assertions.assertEquals(false, differentGroup.equals(referenceA));
    }
}
