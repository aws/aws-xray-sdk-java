package com.amazonaws.xray.strategy.sampling.rule;

import com.amazonaws.xray.strategy.sampling.SamplingResponse;
import java.time.Instant;

public interface Rule {
    SamplingResponse sample(Instant now);
}
