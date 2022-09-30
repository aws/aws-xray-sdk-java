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

package com.amazonaws.xray.lambda;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class SQSMessageHelperTest {
    @Test
    public void testSampled() {
        testTrue("Root=1-632BB806-bd862e3fe1be46a994272793;Sampled=1");
        testTrue("Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1");
        testTrue("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

        testFalse("Root=1-632BB806-bd862e3fe1be46a994272793;Sampled=0");
        testFalse("Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0");
        testFalse("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=0");
    }

    public void testTrue(String traceHeader) {
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setAttributes(Collections.singletonMap("AWSTraceHeader", traceHeader));

        assertTrue(SQSMessageHelper.isSampled(sqsMessage));
    }

    public void testFalse(String traceHeader) {
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setAttributes(Collections.singletonMap("AWSTraceHeader", traceHeader));

        assertFalse(SQSMessageHelper.isSampled(sqsMessage));
    }
}
