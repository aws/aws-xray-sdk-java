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

package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.FacadeSegment;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.listeners.SegmentListener;
import java.net.SocketException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Listener that extracts metrics from Segments and emits them to CloudWatch using a structured log mechanism.
 * Structured logs are sent via UDP to the CloudWatch agent.
 *
 * Configuration of UDP metric emissions is described in {@link com.amazonaws.xray.config.MetricsDaemonConfiguration}.
 *
 * For a list of supported metrics see {@link EMFMetricFormatter}.
 */
public class MetricsSegmentListener implements SegmentListener {
    private static final Log logger = LogFactory.getLog(MetricsSegmentListener.class);
    private static final String AWS_EXECUTION_ENV_NAME = "AWS_EXECUTION_ENV";
    private static final String AWS_LAMBDA_PREFIX = "AWS_Lambda_";

    private MetricEmitter emitter = new NoOpMetricEmitter();

    public MetricsSegmentListener() {
        String awsEnv = System.getenv(AWS_EXECUTION_ENV_NAME);
        try {
            if (awsEnv != null && awsEnv.contains(AWS_LAMBDA_PREFIX)) {
                // Metrics are not supported on Lambda as the root Segment is managed by Lambda and not this SDK.
                logger.info("Metric emissions is not supported on Lambda.");
            } else {
                logger.info("Emitting metrics to the CloudWatch Agent.");
                emitter = new UDPMetricEmitter();
            }

        } catch (SocketException e) {
            logger.error("Unable to construct metrics emitter. No metrics will be published.", e);
        }
    }

    @Override
    public void afterEndSegment(final Segment segment) {
        if (segment != null && !(segment instanceof FacadeSegment)) {
            emitter.emitMetric(segment);
        }
    }
}
