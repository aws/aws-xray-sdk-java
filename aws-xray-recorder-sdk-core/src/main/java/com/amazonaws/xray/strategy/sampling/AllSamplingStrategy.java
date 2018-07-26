package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns true. Use this sampling strategy to trace every request.
 *
 */
public class AllSamplingStrategy implements SamplingStrategy {
    public SamplingResponse shouldTrace(String serviceName, String host, String path, String method) {
        SamplingResponse sampleResponse = new SamplingResponse(true);
        return sampleResponse;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}
