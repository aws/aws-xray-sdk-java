package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns true. Use this sampling strategy to trace every request.
 *
 */
public class AllSamplingStrategy implements SamplingStrategy {
    public boolean shouldTrace(String serviceName, String path, String method) {
        return true;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}
