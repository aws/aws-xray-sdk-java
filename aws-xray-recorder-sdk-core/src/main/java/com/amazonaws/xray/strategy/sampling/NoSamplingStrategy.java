package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns false. Use this sampling strategy to completely disable tracing.
 *
 */
public class NoSamplingStrategy implements SamplingStrategy {
    public SamplingResponse shouldTrace(String serviceName, String host, String path, String method) {
        SamplingResponse sampleResponse = new SamplingResponse(false, "");
        return sampleResponse;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}



