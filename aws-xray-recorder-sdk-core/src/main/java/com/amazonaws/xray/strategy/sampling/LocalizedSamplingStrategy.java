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

import com.amazonaws.xray.strategy.sampling.manifest.SamplingRuleManifest;
import com.amazonaws.xray.strategy.sampling.rule.SamplingRule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LocalizedSamplingStrategy implements SamplingStrategy {
    private static final Log logger =
        LogFactory.getLog(LocalizedSamplingStrategy.class);

    private final boolean forcedSamplingSupport;

    // Visible for other sampling strategies
    static final URL DEFAULT_RULES;

    static {
        URL defaultRules =
            LocalizedSamplingStrategy.class.getResource("/com/amazonaws/xray/strategy/sampling/DefaultSamplingRules.json");
        if (defaultRules == null) {
            throw new IllegalStateException("Could not find DefaultSamplingRules.json on the classpath. This is a packaging bug "
                                            + "- did you correctly shade this library?");
        }
        DEFAULT_RULES = defaultRules;
    }

    private static final ObjectMapper MAPPER =
        new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private static final List<Integer> SUPPORTED_VERSIONS = Arrays.asList(1, 2);

    @Nullable
    private final URL samplingRulesLocation;

    @Nullable
    private List<SamplingRule> rules;
    @Nullable
    private SamplingRule defaultRule;

    public LocalizedSamplingStrategy() {
        this(DEFAULT_RULES, false);
    }

    public LocalizedSamplingStrategy(@Nullable URL ruleLocation) {
        this(ruleLocation, false);
    }

    public LocalizedSamplingStrategy(boolean forcedSamplingSupport) {
        this(DEFAULT_RULES, forcedSamplingSupport);
    }

    public LocalizedSamplingStrategy(@Nullable URL ruleLocation, boolean forcedSamplingSupport) {
        this.samplingRulesLocation = ruleLocation;
        this.forcedSamplingSupport = forcedSamplingSupport;

        SamplingRuleManifest manifest = getRuleManifest(ruleLocation);
        if (manifest != null) {
            defaultRule = manifest.getDefaultRule();
            rules = processRuleManifest(manifest);
        }
    }

    @Nullable
    public URL getSamplingManifestURL() {
        return samplingRulesLocation;
    }

    private static SamplingRuleManifest getRuleManifest(@Nullable URL ruleLocation) {
        if (ruleLocation == null) {
            logger.error("Unable to parse null URL. Falling back to default rule set: "
                         + LocalizedSamplingStrategy.DEFAULT_RULES.getPath());
            return getDefaultRuleManifest();
        }
        try {
            return MAPPER.readValue(ruleLocation, SamplingRuleManifest.class);
        } catch (IOException ioe) {
            logger.error("Unable to parse " + ruleLocation.getPath() + ". Falling back to default rule set: "
                         + LocalizedSamplingStrategy.DEFAULT_RULES.getPath(), ioe);
            return getDefaultRuleManifest();
        }
    }

    private static SamplingRuleManifest getDefaultRuleManifest() {
        try {
            return MAPPER.readValue(LocalizedSamplingStrategy.DEFAULT_RULES, SamplingRuleManifest.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to parse " + LocalizedSamplingStrategy.DEFAULT_RULES + ".", ioe);
        }
    }

    private static List<SamplingRule> processRuleManifest(SamplingRuleManifest manifest) {
        SamplingRule defaultRule = manifest.getDefaultRule();
        if (!SUPPORTED_VERSIONS.contains(manifest.getVersion())) {
            throw newInvalidSamplingRuleManifestException("Manifest version: " + manifest.getVersion() + " is not supported.");
        }
        if (defaultRule != null) {
            if (defaultRule.getUrlPath() != null || defaultRule.getHost() != null || defaultRule.getHttpMethod() != null) {
                throw newInvalidSamplingRuleManifestException(
                    "The default rule must not specify values for url_path, host, or http_method.");
            } else if (defaultRule.getFixedTarget() < 0 || defaultRule.getRate() < 0) {
                throw newInvalidSamplingRuleManifestException(
                    "The default rule must specify non-negative values for fixed_target and rate.");
            }
            List<SamplingRule> rules = manifest.getRules();
            if (rules != null) {
                for (SamplingRule rule : rules) {
                    if (manifest.getVersion() == 1) {
                        rule.setHost(rule.getServiceName());
                    }
                    if (rule.getUrlPath() == null || rule.getHost() == null || rule.getHttpMethod() == null) {
                        throw newInvalidSamplingRuleManifestException(
                            "All rules must have values for url_path, host, and http_method.");
                    } else if (rule.getFixedTarget() < 0 || rule.getRate() < 0) {
                        throw newInvalidSamplingRuleManifestException(
                            "All rules must have non-negative values for fixed_target and rate.");
                    }
                }
                return rules;
            } else {
                return new ArrayList<>();
            }
        } else {
            throw newInvalidSamplingRuleManifestException("A default rule must be provided.");
        }
    }

    private static RuntimeException newInvalidSamplingRuleManifestException(String detail) {
        return new RuntimeException("Invalid sampling rule manifest provided. " + detail);
    }

    @Override
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        if (logger.isDebugEnabled()) {
            logger.debug("Determining shouldTrace decision for:\n\thost: " + samplingRequest.getHost().orElse("") + "\n\tpath: "
                         + samplingRequest.getUrl().orElse("") + "\n\tmethod: " + samplingRequest.getMethod().orElse(""));
        }
        SamplingResponse sampleResponse = new SamplingResponse();
        SamplingRule firstApplicableRule = null;
        if (rules != null) {
            firstApplicableRule = rules.stream()
                                       .filter(rule -> rule.appliesTo(samplingRequest.getHost().orElse(""),
                                                                      samplingRequest.getUrl().orElse(""),
                                                                      samplingRequest.getMethod().orElse("")))
                                       .findFirst().orElse(null);
        }
        sampleResponse.setSampled(firstApplicableRule == null ? shouldTrace(defaultRule) : shouldTrace(firstApplicableRule));
        return sampleResponse;
    }

    private boolean shouldTrace(@Nullable SamplingRule samplingRule) {
        if (logger.isDebugEnabled()) {
            logger.debug("Applicable sampling rule: " + samplingRule);
        }

        if (samplingRule == null) {
            return false;
        }

        if (samplingRule.getReservoir().take()) {
            return true;
        } else {
            return ThreadLocalRandom.current().nextFloat() < samplingRule.getRate();
        }
    }

    @Override
    public boolean isForcedSamplingSupported() {
        return forcedSamplingSupport;
    }
}
