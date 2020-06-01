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

import com.amazonaws.xray.entities.Segment;

/**
 * Writes EMF formatted structured logs to stdout for testing.
 */
public class StdoutMetricEmitter implements MetricEmitter {

    private MetricFormatter formatter;

    public StdoutMetricEmitter() {
        formatter = new EMFMetricFormatter();
    }

    @SuppressWarnings("checkstyle:Regexp")
    @Override
    public void emitMetric(final Segment segment) {
        String output = formatter.formatSegment(segment);
        System.out.println(output);
    }
}
