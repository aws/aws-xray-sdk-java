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

package com.amazonaws.xray.strategy.sampling.manifest;

import com.amazonaws.xray.strategy.sampling.rule.SamplingRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SamplingRuleManifest {
    @Nullable
    private List<SamplingRule> rules;

    @JsonProperty("default") // default is a reserved word
    @Nullable
    private SamplingRule defaultRule;
    private int version;

    /**
     * @return the rules
     */
    @Nullable
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
    @Nullable
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
