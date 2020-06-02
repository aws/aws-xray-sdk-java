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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates metrics based on a Segment.
 *
 * <p>These metrics are published to CloudWatch via a structured log through CloudWatch Logs. These logs are created in the
 * <u>ServiceMetricsSDK</u> log group.</p>
 *
 * <p>In addition to the metrics describe below these logs contain properties to enable correlation with Traces.
 * <ul>
 *    <li><u>Timestamp:</u> The end time of the segment, used for the timestamp of the generated metric.</li>
 *    <li><u>TraceId:</u> The Trace ID</li>
 * </ul>
 *
 * <p>Metrics are published to the <u>ServiceMetrics/SDK</u> namespace with dimensions:</p>
 *  <ul>
 *     <li><u>ServiceType:</u> The Segment Origin
 *     <li><u>ServiceName:</u> The Segment Name</li>
 *  </ul>
 *
 *  <p>The following metrics will be reported:</p>
 *  <ul>
 *      <li><u>Latency:</u> The difference between Start and End time in milliseconds</li>
 *      <li><u>ErrorRate:</u> 1 if the segment is marked as an error, zero otherwise. </li>
 *      <li><u>FaultRate:</u> 1 if the segment is marked as an fault, zero otherwise. </li>
 *      <li><u>ThrottleRate:</u> 1 if the segment is marked as throttled, zero otherwise.</li>
 *      <li><u>OkRate:</u> 1 if no other statuses are set, zero otherwise.</li>
 *  </ul>
 *
 * <p>Rate metrics above may be used with the CloudWatch AVG statistic to get a percentage of requests in a category or may be
 * used as a SUM. COUNT of Latency represents the total count of invocations of this segment.</p>
 */
public class EMFMetricFormatter implements MetricFormatter {

    private static final Log logger = LogFactory.getLog(EMFMetricFormatter.class);

    private static final String EMF_FORMAT = "{\"Timestamp\":%d,\"log_group_name\":\"ServiceMetricsSDK\",\"CloudWatchMetrics\":"
                                           + "[{\"Metrics\":[{\"Name\":\"ErrorRate\",\"Unit\":\"None\"},{\"Name\":\"FaultRate\","
                                           + "\"Unit\":\"None\"},{\"Name\":\"ThrottleRate\",\"Unit\":\"None\"},"
                                           + "{\"Name\":\"OkRate\",\"Unit\":\"None\"},{\"Name\":\"Latency\","
                                           + "\"Unit\":\"Milliseconds\"}],\"Namespace\":\"ServiceMetrics/SDK\","
                                           + "\"Dimensions\":[[\"ServiceType\",\"ServiceName\"]]}],\"Latency\":%.3f,"
                                           + "\"ErrorRate\":%d,\"FaultRate\":%d,\"ThrottleRate\":%d,\"OkRate\":%d,"
                                           + "\"TraceId\":\"%s\",\"ServiceType\":\"%s\",\"ServiceName\":\"%s\","
                                           + "\"Version\":\"0\"}";

    @Override
    public String formatSegment(final Segment segment) {
        int errorRate = segment.isError() ? 1 : 0;
        int faultRate = segment.isFault() ? 1 : 0;
        int throttleRate = segment.isThrottle() ? 1 : 0;
        int okRate = (errorRate + faultRate + throttleRate) > 0 ? 0 : 1;
        double duration = (segment.getEndTime() - segment.getStartTime()) * 1000;

        long endTimeMillis = (long) (segment.getEndTime() * 1000);

        String json = String.format(EMF_FORMAT,
                endTimeMillis,
                duration,
                errorRate,
                faultRate,
                throttleRate,
                okRate,
                segment.getTraceId().toString(),
                segment.getOrigin(),
                segment.getName());

        if (logger.isDebugEnabled()) {
            logger.debug("Formatted segment " + segment.getName() + " as EMF: " + json);
        }

        return json;
    }
}
