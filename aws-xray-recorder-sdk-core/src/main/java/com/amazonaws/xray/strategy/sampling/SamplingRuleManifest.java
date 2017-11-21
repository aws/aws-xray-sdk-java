package com.amazonaws.xray.strategy.sampling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SamplingRuleManifest {
    private List<SamplingRule> rules;

    @JsonProperty("default") // default is a reserved word
    private SamplingRule defaultRule;
    private int version;

    /**
     * @return the rules
     */
    public List<SamplingRule> getRules() {
        return rules;
    }

    /**
     * @param rules the rules to set
     */
    public void setRules(List<SamplingRule> rules) {
        this.rules = rules;
    }

    /**
     * @return the defaultRule
     */
    public SamplingRule getDefaultRule() {
        return defaultRule;
    }

    /**
     * @param defaultRule the defaultRule to set
     */
    public void setDefaultRule(SamplingRule defaultRule) {
        this.defaultRule = defaultRule;
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(int version) {
        this.version = version;
    }
}
