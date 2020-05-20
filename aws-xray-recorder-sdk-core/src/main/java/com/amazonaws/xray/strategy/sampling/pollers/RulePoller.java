package com.amazonaws.xray.strategy.sampling.pollers;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.services.xray.model.GetSamplingRulesRequest;
import com.amazonaws.services.xray.model.GetSamplingRulesResult;
import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.services.xray.model.SamplingRuleRecord;
import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;

public class RulePoller {
    private static final Log logger = LogFactory.getLog(RulePoller.class);

    private static final long PERIOD = 300; // Seconds
    private static final long MAX_JITTER = 5; // Seconds

    private final UnsignedXrayClient client;
    private final CentralizedManifest manifest;
    private final Clock clock;
    private final ScheduledExecutorService executor;

    /**
     * @deprecated Use {@link #RulePoller(UnsignedXrayClient, CentralizedManifest, Clock)}.
     */
    @Deprecated
    public RulePoller(CentralizedManifest manifest, AWSXRay unused, Clock clock) {
        this(new UnsignedXrayClient(), manifest, clock);
    }

    public RulePoller(UnsignedXrayClient client, CentralizedManifest manifest, Clock clock) {
        this.client = client;
        this.manifest = manifest;
        this.clock = clock;
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            try {
                pollRule();
            } catch (Throwable t) {
                logger.error("Encountered error polling GetSamplingRules: ", t);
                // Propagate if Error so executor stops executing.
                // TODO(anuraaga): Many Errors aren't fatal, this should probably be more restricted, e.g.
                // https://github.com/openzipkin/brave/blob/master/brave/src/main/java/brave/internal/Throwables.java
                if(t instanceof Error) { throw t; }
            }
        }, 0, getIntervalWithJitter(), TimeUnit.SECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // Visible for testing
    ScheduledExecutorService getExecutor() {
        return executor;
    }

    private void pollRule() {
        Instant now = clock.instant();

        logger.info("Polling sampling rules.");
        GetSamplingRulesRequest req = new GetSamplingRulesRequest();
        GetSamplingRulesResult records = client.getSamplingRules(req);
        List<SamplingRule> rules = records.getSamplingRuleRecords()
                .stream()
                .map(SamplingRuleRecord::getSamplingRule)
                .filter(CentralizedRule::isValid)
                .collect(Collectors.toList());

        manifest.putRules(rules, now);
    }

    private long getIntervalWithJitter() {
        Rand random = new RandImpl();
        return Math.round(random.next() * MAX_JITTER) + PERIOD;
    }
}
