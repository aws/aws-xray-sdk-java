package com.amazonaws.xray.strategy.sampling.pollers;

import com.amazonaws.services.xray.model.GetSamplingRulesRequest;
import com.amazonaws.services.xray.model.GetSamplingRulesResult;
import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RulePoller {
    private static Log logger = LogFactory.getLog(RulePoller.class);

    private static final long PERIOD = 300; // Seconds
    private static final long MAX_JITTER = 5; // Seconds


    private AWSXRay client;
    private Clock clock;
    private CentralizedManifest manifest;

    public RulePoller(CentralizedManifest manifest, AWSXRay client, Clock clock) {
        this.manifest = manifest;
        this.client = client;
        this.clock = clock;
    }

    public void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                pollRule();
            } catch (Exception ex) {
                logger.error("Encountered error polling GetSamplingRules: ", ex);
            }
        }, 0, getJitterInterval(), TimeUnit.SECONDS);
    }


    private void pollRule() {
        Instant now = clock.instant();

        GetSamplingRulesRequest req = new GetSamplingRulesRequest();
        GetSamplingRulesResult records = client.getSamplingRules(req);
        List<SamplingRule> rules = records.getSamplingRuleRecords()
                .stream()
                .map(r -> r.getSamplingRule())
                .filter(r -> CentralizedRule.isValid(r))
                .collect(Collectors.toList());

        manifest.putRules(rules, now);
    }

    private long getJitterInterval() {
        Rand random = new RandImpl();
        long interval = Math.round(random.next() * MAX_JITTER) + PERIOD;
        return interval;
    }
}
