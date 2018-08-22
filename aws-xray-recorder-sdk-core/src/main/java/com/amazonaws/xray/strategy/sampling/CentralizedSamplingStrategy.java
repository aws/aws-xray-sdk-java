package com.amazonaws.xray.strategy.sampling;

import com.amazonaws.services.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.pollers.RulePoller;
import com.amazonaws.xray.strategy.sampling.pollers.TargetPoller;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.DatatypeConverter;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

public class CentralizedSamplingStrategy implements SamplingStrategy {
    private static Log logger = LogFactory.getLog(TargetPoller.class);
    // Initialize random ClientID. We use the same ClientID for all GetSamplingTargets calls. Conflicts are avoided
    // because IDs are scoped to a single account.
    private static String clientID;
    static {
        SecureRandom rand = new SecureRandom();
        byte[] bytes = new byte[12];
        rand.nextBytes(bytes);
        clientID = DatatypeConverter.printHexBinary(bytes);
    }

    private boolean isStarted = false;
    private CentralizedManifest manifest;
    private LocalizedSamplingStrategy fallback;
    private RulePoller rulePoller;
    private TargetPoller targetPoller;
    private AWSXRay client;

    public CentralizedSamplingStrategy() {
        this.manifest = new CentralizedManifest();
        this.fallback = new LocalizedSamplingStrategy();
        this.client = XRayClient.newClient();
        this.rulePoller = new RulePoller(manifest, client, Clock.systemUTC());
        this.targetPoller = new TargetPoller(manifest, client, Clock.systemUTC());
    }

    public CentralizedSamplingStrategy(URL ruleLocation) {
        this.manifest = new CentralizedManifest();
        this.fallback = new LocalizedSamplingStrategy(ruleLocation);
        this.client = XRayClient.newClient();
        this.rulePoller = new RulePoller(manifest, client, Clock.systemUTC());
        this.targetPoller = new TargetPoller(manifest, client, Clock.systemUTC());
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

        if (!samplingRequest.getServiceType().isPresent()) {
            samplingRequest.setServiceType(AWSXRayRecorderBuilder.defaultRecorder().getOrigin());
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

    public static String getClientID() {
        return clientID;
    }

    // Adding synchronized to this method will makes it thread-safe.
    private synchronized void startPoller() {
        rulePoller.start();
        targetPoller.start();
        this.isStarted = true;
    }

    @Override
    public boolean isForcedSamplingSupported() {
        //TODO address this
        return false;
    }

}
