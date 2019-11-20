package com.amazonaws.xray.entities;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;

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

    @Test
    public void testEqualityPositive() {
        assertEquals(true, referenceA.equals(referenceB));
        assertEquals(true, referenceB.equals(referenceA));
        assertEquals(true, referenceA.equals(referenceA));
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
