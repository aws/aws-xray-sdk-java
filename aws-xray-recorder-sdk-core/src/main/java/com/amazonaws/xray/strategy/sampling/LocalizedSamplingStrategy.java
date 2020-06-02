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

public class LocalizedSamplingStrategy implements SamplingStrategy {
    private static final Log logger =
        LogFactory.getLog(LocalizedSamplingStrategy.class);

    private static final URL DEFAULT_RULES =
        LocalizedSamplingStrategy.class.getResource("/com/amazonaws/xray/strategy/sampling/DefaultSamplingRules.json");

    private static final ObjectMapper MAPPER =
        new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private URL samplingRulesLocation;
    private List<SamplingRule> rules;
    private SamplingRule defaultRule;

    private List<Integer> supportedVersions = Arrays.asList(1, 2);

    public LocalizedSamplingStrategy() {
        this(DEFAULT_RULES);
    }

    public LocalizedSamplingStrategy(URL ruleLocation) {
        this.samplingRulesLocation = ruleLocation;
        processRuleManifest(getRuleManifest(ruleLocation));
    }

    public URL getSamplingManifestURL() {
        return samplingRulesLocation;
    }

    private SamplingRuleManifest getRuleManifest(URL ruleLocation) {
        if (null == ruleLocation) {
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

    private SamplingRuleManifest getDefaultRuleManifest() {
        try {
            return MAPPER.readValue(LocalizedSamplingStrategy.DEFAULT_RULES, SamplingRuleManifest.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to parse " + LocalizedSamplingStrategy.DEFAULT_RULES + ".", ioe);
        }
    }

    private void processRuleManifest(SamplingRuleManifest manifest) {
        if (manifest == null) {
            return;
        }
        defaultRule = manifest.getDefaultRule();
        if (!supportedVersions.contains(manifest.getVersion())) {
            throwInvalidSamplingRuleManifestException("Manifest version: " + manifest.getVersion() + " is not supported.");
        }
        if (null != defaultRule) {
            if (null != defaultRule.getUrlPath() || null != defaultRule.getHost() || null != defaultRule.getHttpMethod()) {
                throwInvalidSamplingRuleManifestException(
                    "The default rule must not specify values for url_path, host, or http_method.");
            } else if (defaultRule.getFixedTarget() < 0 || defaultRule.getRate() < 0) {
                throwInvalidSamplingRuleManifestException(
                    "The default rule must specify non-negative values for fixed_target and rate.");
            }
            if (null != manifest.getRules()) {
                manifest.getRules().forEach(rule -> {
                    if (manifest.getVersion() == 1) {
                        rule.setHost(rule.getServiceName());
                    }
                    if (null == rule.getUrlPath() || null == rule.getHost() || null == rule.getHttpMethod()) {
                        throwInvalidSamplingRuleManifestException(
                            "All rules must have values for url_path, host, and http_method.");
                    } else if (rule.getFixedTarget() < 0 || rule.getRate() < 0) {
                        throwInvalidSamplingRuleManifestException(
                            "All rules must have non-negative values for fixed_target and rate.");
                    }
                });
                rules = manifest.getRules();
            } else {
                rules = new ArrayList<>();
            }
        } else {
            throwInvalidSamplingRuleManifestException("A default rule must be provided.");
        }
    }

    private void throwInvalidSamplingRuleManifestException(String detail) {
        throw new RuntimeException("Invalid sampling rule manifest provided. " + detail);
    }

    @Override
    public SamplingResponse shouldTrace(SamplingRequest samplingRequest) {
        if (logger.isDebugEnabled()) {
            logger.debug("Determining shouldTrace decision for:\n\thost: " + samplingRequest.getHost().orElse("") + "\n\tpath: "
                         + samplingRequest.getUrl().orElse("") + "\n\tmethod: " + samplingRequest.getMethod().orElse(""));
        }
        SamplingResponse sampleResponse = new SamplingResponse();
        SamplingRule firstApplicableRule = null;
        if (null != rules) {
            firstApplicableRule = rules.stream()
                                       .filter(rule -> rule.appliesTo(samplingRequest.getHost().orElse(""),
                                                                      samplingRequest.getUrl().orElse(""),
                                                                      samplingRequest.getMethod().orElse("")))
                                       .findFirst().orElse(null);
        }
        sampleResponse.setSampled(null == firstApplicableRule ? shouldTrace(defaultRule) : shouldTrace(firstApplicableRule));
        return sampleResponse;
    }

    private boolean shouldTrace(SamplingRule samplingRule) {
        if (logger.isDebugEnabled()) {
            logger.debug("Applicable sampling rule: " + samplingRule);
        }

        if (samplingRule.getReservoir().take()) {
            return true;
        } else {
            return ThreadLocalRandom.current().nextFloat() < samplingRule.getRate();
        }
    }

    @Override
    public boolean isForcedSamplingSupported() {
        //TODO address this
        return false;
    }
}
