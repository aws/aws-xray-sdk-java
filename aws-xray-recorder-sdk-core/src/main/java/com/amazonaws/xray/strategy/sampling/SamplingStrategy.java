package com.amazonaws.xray.strategy.sampling;

public interface SamplingStrategy {
    public boolean shouldTrace(String serviceName, String path, String method);

    /**
     * Returns whether or not this sampling strategy supports 'forced sampling'.
     *
     * Forced sampling allows a segment's initial non-sampled decision to be later overriden to sampled. Supporting this feature requires that all segments, sampled or otherwise, be kept in memory for
     * the duration of their existence. Not supporting this feature saves memory and computational capacity.
     *
     * @return whether or not forced sampling is supported
     */
    public boolean isForcedSamplingSupported();
}
