package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns true. Use this sampling strategy to trace every request.
 *
 */
public class AllSamplingStrategy implements SamplingStrategy {
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        SamplingResponse sampleResponse = new SamplingResponse(true);
        return sampleResponse;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}
