package com.amazonaws.xray.strategy.sampling;

/**
 * A sampling strategy for which {@code shouldTrace} always returns false. Use this sampling strategy to completely disable tracing.
 *
 */
public class NoSamplingStrategy implements SamplingStrategy {
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        SamplingResponse sampleResponse = new SamplingResponse(false, "");
        return sampleResponse;
    }

    public boolean isForcedSamplingSupported() {
        return false;
    }
}



