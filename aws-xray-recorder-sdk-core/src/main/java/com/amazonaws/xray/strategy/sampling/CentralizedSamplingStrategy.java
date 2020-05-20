package com.amazonaws.xray.strategy.sampling;

import java.net.URL;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.pollers.RulePoller;
import com.amazonaws.xray.strategy.sampling.pollers.TargetPoller;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import com.amazonaws.xray.utils.ByteUtils;

public class CentralizedSamplingStrategy implements SamplingStrategy {
    private static final Log logger = LogFactory.getLog(TargetPoller.class);
    // Initialize random ClientID. We use the same ClientID for all GetSamplingTargets calls. Conflicts are avoided
    // because IDs are scoped to a single account.
    private static final String clientID;
    static {
        SecureRandom rand = new SecureRandom();
        byte[] bytes = new byte[12];
        rand.nextBytes(bytes);
        clientID = ByteUtils.byteArrayToHexString(bytes);
    }

    private final CentralizedManifest manifest;
    private final LocalizedSamplingStrategy fallback;
    private final RulePoller rulePoller;
    private final TargetPoller targetPoller;

    private boolean isStarted = false;

    public CentralizedSamplingStrategy() {
        this.manifest = new CentralizedManifest();
        this.fallback = new LocalizedSamplingStrategy();
        UnsignedXrayClient client = new UnsignedXrayClient();
        this.rulePoller = new RulePoller(client, manifest, Clock.systemUTC());
        this.targetPoller = new TargetPoller(client, manifest, Clock.systemUTC());
    }

    public CentralizedSamplingStrategy(URL ruleLocation) {
        this.manifest = new CentralizedManifest();
        this.fallback = new LocalizedSamplingStrategy(ruleLocation);
        UnsignedXrayClient client = new UnsignedXrayClient();
        this.rulePoller = new RulePoller(client, manifest, Clock.systemUTC());
        this.targetPoller = new TargetPoller(client, manifest, Clock.systemUTC());
    }

    @Override
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        if (!isStarted) {
            startPoller();
        }
        SamplingResponse sampleResponse;
        if (logger.isDebugEnabled()) {
            logger.debug("Determining shouldTrace decision for:\n\tserviceName: " + samplingRequest.getService().orElse("") + "\n\thost: " + samplingRequest.getHost().orElse("") + "\n\tpath: " + samplingRequest.getUrl().orElse("") + "\n\tmethod: " + samplingRequest.getMethod().orElse("") + "\n\tserviceType: " + samplingRequest.getServiceType().orElse(""));
        }

        if (manifest.isExpired(Instant.now())) {
            logger.debug("Centralized sampling data expired. Using fallback sampling strategy.");
            return fallback.shouldTrace(samplingRequest);
        }

        for (CentralizedRule rule : manifest.getRules().values()) {
            boolean applicable = rule.match(samplingRequest);
            if (!applicable) {
                continue;
            }
            logger.debug("Applicable rule:" + rule.getName());
            return rule.sample(Instant.now());
        }

        // Match against default rule
        CentralizedRule dRule = manifest.getDefaultRule();
        if (dRule != null) {
            logger.debug("Applicable default rule: " + dRule.getName());
            return dRule.sample(Instant.now());
        }

        logger.debug("Centralized default sampling rule unavailable. Using fallback sampling strategy.");
        sampleResponse = fallback.shouldTrace(samplingRequest);
        return sampleResponse;
    }

    @Override
    /**
     * Shutdown all poller threads immediately regardless of the pending work for clean exit.
     */
    public void shutdown() {
        rulePoller.shutdown();
        targetPoller.shutdown();
    }

    public static String getClientID() {
        return clientID;
    }

    // This method needs to be thread-safe.
    private synchronized void startPoller() {
        if (isStarted) { return; }
        rulePoller.start();
        targetPoller.start();
        isStarted = true;
    }

    @Override
    public boolean isForcedSamplingSupported() {
        //TODO address this
        return false;
    }

}
