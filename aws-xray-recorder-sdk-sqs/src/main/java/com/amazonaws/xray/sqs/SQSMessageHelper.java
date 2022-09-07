package com.amazonaws.xray.sqs;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

public class SQSMessageHelper {
    public static boolean isSampled(SQSEvent.SQSMessage message) {
        return message.getAttributes().get("AWSTraceHeader").contains(";Sampled=1");
    }
}