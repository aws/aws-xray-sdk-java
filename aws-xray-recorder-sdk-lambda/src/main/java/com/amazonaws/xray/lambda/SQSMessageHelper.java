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

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public final class SQSMessageHelper {
    private SQSMessageHelper() {
    }

    public static boolean isSampled(SQSEvent.SQSMessage message) {
        if (!message.getAttributes().containsKey("AWSTraceHeader")) {
            return false;
        }

        return message.getAttributes().get("AWSTraceHeader").contains("Sampled=1");
    }
}
