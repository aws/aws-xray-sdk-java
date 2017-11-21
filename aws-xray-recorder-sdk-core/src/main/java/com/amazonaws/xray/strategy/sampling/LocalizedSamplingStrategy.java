package com.amazonaws.xray.strategy.sampling;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;


public class LocalizedSamplingStrategy implements SamplingStrategy {
    private static final Log logger =
        LogFactory.getLog(LocalizedSamplingStrategy.class);

    private static final URL DEFAULT_RULES = LocalizedSamplingStrategy.class.getResource("/com/amazonaws/xray/strategy/sampling/DefaultSamplingRules.json");

    private List<SamplingRule> rules;
    private SamplingRule defaultRule;

    private ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    public LocalizedSamplingStrategy() {
        this(DEFAULT_RULES);
    }

    public LocalizedSamplingStrategy(URL ruleLocation) {
        processRuleManifest(getRuleManifest(ruleLocation));
    }

    private SamplingRuleManifest getRuleManifest(URL ruleLocation) {
        if (null == ruleLocation) {
            logger.error("Unable to parse null URL. Falling back to default rule set: " + LocalizedSamplingStrategy.DEFAULT_RULES.getPath());
            return getDefaultRuleManifest();
        }
        try {
            return mapper.readValue(ruleLocation, SamplingRuleManifest.class);
        } catch (IOException ioe) {
            logger.error("Unable to parse " + ruleLocation.getPath() + ". Falling back to default rule set: " + LocalizedSamplingStrategy.DEFAULT_RULES.getPath(), ioe);
            return getDefaultRuleManifest();
        }
    }

    private SamplingRuleManifest getDefaultRuleManifest() {
        try {
            return mapper.readValue(LocalizedSamplingStrategy.DEFAULT_RULES, SamplingRuleManifest.class);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to parse " + LocalizedSamplingStrategy.DEFAULT_RULES + ".", ioe);
        }
    }

    private void processRuleManifest(SamplingRuleManifest manifest) {
        if (null != manifest) {
            defaultRule = manifest.getDefaultRule();
            if (null != defaultRule) {
                if (null != defaultRule.getUrlPath() || null != defaultRule.getServiceName() || null != defaultRule.getHttpMethod()) {
                    throwInvalidSamplingRuleManifestException("The default rule must not specify values for url_path, service_name, or http_method.");
                } else if (defaultRule.getFixedTarget() < 0 || defaultRule.getRate() < 0) {
                    throwInvalidSamplingRuleManifestException("The default rule must specify non-negative values for fixed_target and rate.");
                } else if (manifest.getVersion() != 1) {
                    throwInvalidSamplingRuleManifestException("Manifest version: " + manifest.getVersion() + " is not supported.");
                }
                if (null != manifest.getRules()) {
                    manifest.getRules().forEach( (rule) -> {
                        if (null == rule.getUrlPath() || null == rule.getServiceName() || null == rule.getHttpMethod()) {
                            throwInvalidSamplingRuleManifestException("All rules must have values for url_path, service_name, and http_method.");
                        } else if (rule.getFixedTarget() < 0 || rule.getRate() < 0) {
                            throwInvalidSamplingRuleManifestException("All rules must have non-negative values for fixed_target and rate.");
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
    }

    private void throwInvalidSamplingRuleManifestException(String detail) {
        throw new RuntimeException("Invalid sampling rule manifest provided. " + detail);
    }

    @Override
    public boolean shouldTrace(String serviceName, String path, String method) {
        if (logger.isDebugEnabled()) {
            logger.debug("Determining shouldTrace decision for:\n\tserviceName: " + serviceName + "\n\tpath: " + path + "\n\tmethod: " + method);
        }

        SamplingRule firstApplicableRule = null;
        if (null != rules) {
            firstApplicableRule = rules.stream().filter( rule -> rule.appliesTo(serviceName, path, method) ).findFirst().orElse(null);
        }
        return null == firstApplicableRule ? shouldTrace(defaultRule) : shouldTrace(firstApplicableRule);
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
