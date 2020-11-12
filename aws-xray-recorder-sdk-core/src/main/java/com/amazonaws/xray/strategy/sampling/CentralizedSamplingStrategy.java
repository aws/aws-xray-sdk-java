/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.xray.strategy.sampling;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import com.amazonaws.xray.strategy.sampling.pollers.RulePoller;
import com.amazonaws.xray.strategy.sampling.pollers.TargetPoller;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import com.amazonaws.xray.utils.ByteUtils;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CentralizedSamplingStrategy implements SamplingStrategy {
    private static final Log logger = LogFactory.getLog(TargetPoller.class);
    // Initialize random ClientID. We use the same ClientID for all GetSamplingTargets calls. Conflicts are avoided
    // because IDs are scoped to a single account.
    private static final String clientID;

    static {
        SecureRandom rand = new SecureRandom();
        byte[] bytes = new byte[12];
        rand.nextBytes(bytes);
        String clientId = ByteUtils.byteArrayToHexString(bytes);
        if (clientId == null) {
            // Satisfy checker framework.
            throw new IllegalStateException();
        }
        clientID = clientId;
    }

    private final CentralizedManifest manifest;
    private final LocalizedSamplingStrategy fallback;
    private final RulePoller rulePoller;
    private final TargetPoller targetPoller;
    private final boolean forcedSamplingSupport;

    private boolean isStarted = false;

    public CentralizedSamplingStrategy() {
        this(LocalizedSamplingStrategy.DEFAULT_RULES, false);
    }

    public CentralizedSamplingStrategy(@Nullable URL ruleLocation) {
        this(ruleLocation, false);
    }

    public CentralizedSamplingStrategy(boolean forcedSamplingSupport) {
        this(LocalizedSamplingStrategy.DEFAULT_RULES, forcedSamplingSupport);
    }

    public CentralizedSamplingStrategy(@Nullable URL ruleLocation, boolean forcedSamplingSupport) {
        this.manifest = new CentralizedManifest();
        this.fallback = new LocalizedSamplingStrategy(ruleLocation);
        UnsignedXrayClient client = new UnsignedXrayClient();
        this.rulePoller = new RulePoller(client, manifest, Clock.systemUTC());
        this.targetPoller = new TargetPoller(client, manifest, Clock.systemUTC());
        this.forcedSamplingSupport = forcedSamplingSupport;
    }

    @Nullable
    public URL getSamplingManifestURL() {
        return this.fallback.getSamplingManifestURL();
    }

    @Override
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        if (!isStarted) {
            startPoller();
        }
        SamplingResponse sampleResponse;
        if (logger.isDebugEnabled()) {
            logger.debug("Determining shouldTrace decision for:\n\tserviceName: " + samplingRequest.getService().orElse("")
                         + "\n\thost: " + samplingRequest.getHost().orElse("") + "\n\tpath: "
                         + samplingRequest.getUrl().orElse("") + "\n\tmethod: " + samplingRequest.getMethod().orElse("")
                         + "\n\tserviceType: " + samplingRequest.getServiceType().orElse(""));
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

            if (logger.isDebugEnabled()) {
                logger.debug("Applicable rule:" + rule.getName());
            }

            SamplingResponse response = rule.sample(Instant.now());

            if (logger.isDebugEnabled()) {
                logger.debug("Segment " + samplingRequest.getService().orElse("") + " has" +
                    (response.isSampled() ? " " : " NOT ") + "been sampled.");
            }

            return response;
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
        return forcedSamplingSupport;
    }
}
