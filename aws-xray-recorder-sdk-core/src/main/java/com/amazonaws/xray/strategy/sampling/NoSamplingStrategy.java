package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns false. Use this sampling strategy to completely disable tracing.
 *
 */
public class NoSamplingStrategy implements SamplingStrategy {
    public boolean shouldTrace(String serviceName, String path, String method) {
        return false;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}



