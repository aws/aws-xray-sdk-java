package com.amazonaws.xray.metrics;

import com.amazonaws.xray.entities.Segment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;

/**
 * A basic implementation of CloudWatch's EMF structured log format.
 */
public class EMFMetricFormatter implements MetricFormatter {

    private static final Log logger = LogFactory.getLog(EMFMetricFormatter.class);

    private static final String EMF_FORMAT="{\"Timestamp\":%d,\"log_group_name\":\"XrayApplicationMetrics\",\"CloudWatchMetrics\":[{\"Metrics\":[{\"Name\":\"ErrorRate\",\"Unit\":\"None\"},{\"Name\":\"FaultRate\",\"Unit\":\"None\"},{\"Name\":\"ThrottleRate\",\"Unit\":\"None\"},{\"Name\":\"Latency\",\"Unit\":\"Milliseconds\"}],\"Namespace\":\"Observability\",\"Dimensions\":[[\"ServiceType\",\"ServiceName\"]]}],\"Latency\":%.3f,\"ErrorRate\":%d,\"FaultRate\":%d,\"ThrottleRate\":%d,\"TraceId\":\"%s\",\"ServiceType\":\"%s\",\"ServiceName\":\"%s\",\"Version\":\"0\"}";

    @Override
    /**
     * Formats a segment into an EMF string suitable for use with CloudWatch Logs.
     * <p/>
     * This representation extracts properties from the segment as follows:
     *  <ul>
     *      <li><u>Timestamp:</u> The end time of the segment, used for the timestamp of the generated metric.</li>
     *      <li><u>Namespace:</u> TODO TBD</li>
     *      <li><u>Dimensions:</u> ServiceType: The Segment Origin, ServiceName: The Segment Name</li>
     *      <li><u>TraceId:</u> The Trace ID</li>
     *      <li><u>Latency:</u> The difference between Start and End time in milliseconds</li>
     *      <li><u>ErrorRate:</u> 1 if the segment is marked as an error, zero otherwise. </li>
     *      <li><u>FaultRate:</u> 1 if the segment is marked as an fault, zero otherwise. </li>
     *      <li><u>ThrottleRate:</u> 1 if the segment is marked as throttled, zero otherwise.</li>
     *  </ul>
     *  <p/>
     * Rate metrics above may be used with the CloudWatch AVG statistic to get a percentage of requests in a category or may be used as a SUM. COUNT of Latency represents the total count of invocations of this segment.
     */
    public String formatSegment(final Segment segment) {
        int errorRate = segment.isError() ? 1 : 0;
        int faultRate = segment.isFault() ? 1 : 0;
        int throttleRate = segment.isThrottle() ? 1 : 0;
        double duration = (segment.getEndTime() - segment.getStartTime()) * 1000;

        long endTimeMillis = new Double(segment.getEndTime() * 1000).longValue();

        String json = String.format(EMF_FORMAT,
                endTimeMillis,
                duration,
                errorRate,
                faultRate,
                throttleRate,
                segment.getTraceId().toString(),
                segment.getOrigin(),
                segment.getName());

        if(logger.isDebugEnabled()) {
            logger.debug("Formatted segment " + segment.getName() + " as EMF: " + json);
        }

        return json;
    }
}
