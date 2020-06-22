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

package com.amazonaws.xray.spring.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Segment;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BaseAbstractXRayInterceptorTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    static class ImplementedXRayInterceptor extends BaseAbstractXRayInterceptor {
        @Override
        protected void xrayEnabledClasses() {
        }
    }

    private BaseAbstractXRayInterceptor xRayInterceptor = new ImplementedXRayInterceptor();

    @Mock
    private AWSXRayRecorder mockRecorder;

    @Mock
    private ProceedingJoinPoint mockPjp;

    @Mock
    private Signature mockSignature;

    @Before
    public void setup() {
        AWSXRay.setGlobalRecorder(mockRecorder);

        when(mockPjp.getArgs()).thenReturn(new Object[] {});
        when(mockPjp.getSignature()).thenReturn(mockSignature);
        when(mockSignature.getName()).thenReturn("testSpringName");
    }

    @Test
    public void testNullSegmentOnProcessFailure() throws Throwable {
        // Test to ensure that getCurrentSegment()/getCurrentSegmentOptional() don't throw NPEs when customers are using
        // the Log context missing strategy during the processXRayTrace call.
        Exception expectedException = new RuntimeException("An intended exception");
        // Cause an intended exception to be thrown so that getCurrentSegmentOptional() is called.
        when(mockRecorder.beginSubsegment(any())).thenThrow(expectedException);

        when(mockRecorder.getCurrentSegmentOptional()).thenReturn(Optional.empty());
        when(mockRecorder.getCurrentSegment()).thenReturn(null);

        try {
            xRayInterceptor.processXRayTrace(mockPjp);
        } catch (Exception e) {
            // A null pointer exception here could potentially indicate that a call to getCurrentSegment() is returning null.
            // i.e. there is another exception other than our intended exception that is thrown that's not supposed to be thrown.
            assertNotEquals(NullPointerException.class, e.getClass());
            assertEquals(expectedException.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testSegmentOnProcessFailure() throws Throwable {
        // Test to ensure that the exception is populated to the segment when an error occurs during the
        // processXRayTrace call.
        Exception expectedException = new RuntimeException("An intended exception");
        // Cause an intended exception to be thrown so that getCurrentSegmentOptional() is called.
        when(mockRecorder.beginSubsegment(any())).thenThrow(expectedException);

        Segment mockSegment = mock(Segment.class);
        when(mockRecorder.getCurrentSegmentOptional()).thenReturn(Optional.of(mockSegment));
        when(mockRecorder.getCurrentSegment()).thenReturn(mockSegment);

        try {
            xRayInterceptor.processXRayTrace(mockPjp);
        } catch (Exception e) {
            verify(mockSegment).addException(expectedException);
        }
    }
}
