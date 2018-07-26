package com.amazonaws.xray.strategy.sampling;

import java.util.Optional;

/**
 * Represents the sampling decision output by the sampler. Used by the SDK to
 * decide whether or not a segment should be emitted.
 */
public class SamplingResponse {

    private boolean sampled;

    private String ruleName;

    /**
     * @param sampled
     *            the boolean sampling decision
     * @param ruleName
     *            the name of the rule used to make the sampling decision
     */
    public SamplingResponse(boolean sampled, String ruleName) {
        this.sampled = sampled;
        this.ruleName = ruleName;
    }

    public SamplingResponse(String ruleName) {
        this.ruleName = ruleName;
    }

    public SamplingResponse(boolean sampled) { this.sampled = sampled; }

    public SamplingResponse() {}

    public boolean isSampled() {
        return sampled;
    }

    public Optional<String> getRuleName() {
        return Optional.ofNullable(ruleName);
    }

    public SamplingResponse setSampled(boolean sampled) {
        this.sampled = sampled;
        return this;
    }

}
