package com.amazonaws.xray.strategy.sampling;

import java.net.URL;

public interface SamplingStrategy {
    SamplingResponse shouldTrace(SamplingRequest sampleRequest);

    /**
     * Returns whether or not this sampling strategy supports 'forced sampling'.
     *
     * Forced sampling allows a segment's initial non-sampled decision to be later overriden to sampled. Supporting this feature requires that all segments, sampled or otherwise, be kept in memory for
     * the duration of their existence. Not supporting this feature saves memory and computational capacity.
     *
     * @return whether or not forced sampling is supported
     */
    boolean isForcedSamplingSupported();

    /**
     * Shutdown additional resources created by advanced sampling strategies.
     */
    default void shutdown() {}
}
