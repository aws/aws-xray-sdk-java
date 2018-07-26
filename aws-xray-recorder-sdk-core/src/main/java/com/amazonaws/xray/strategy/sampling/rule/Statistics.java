package com.amazonaws.xray.strategy.sampling.rule;

/**
 * Per-Rule statistics maintained by the sampler. Used for making sampling
 * decisions and for reporting rule usage to X-Ray.
 */

public class Statistics {

    private int requests;

    private int sampled;

    private int borrowed;

    public void reset() {
        requests = 0;
        sampled = 0;
        borrowed = 0;
    }

    public void incRequest() {
        requests++;
    }

    public void incSampled() {
        sampled++;
    }

    public void incBorrowed() {
        borrowed++;
    }

    public int getRequests() {
        return requests;
    }

    public int getSampled() {
        return sampled;
    }

    public int getBorrowed() {
        return borrowed;
    }

}
