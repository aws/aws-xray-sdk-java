package com.amazonaws.xray.strategy.sampling.manifest;

import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.rule.Rule;

import java.time.Instant;

public interface Manifest {
    Rule match(SamplingRequest req, Instant now);
}
