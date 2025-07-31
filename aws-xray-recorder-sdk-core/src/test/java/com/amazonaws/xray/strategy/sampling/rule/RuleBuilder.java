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

package com.amazonaws.xray.strategy.sampling.rule;

import com.amazonaws.xray.strategy.sampling.GetSamplingRulesResponse.SamplingRule;
import java.util.Map;

public final class RuleBuilder {
    private RuleBuilder() {
        throw new UnsupportedOperationException("Utility class for testing.");
    }

    public static class RuleParams {
        public String name;
        public int priority = 1;
        public int reservoirSize = 10;
        public double fixedRate = 0.05;
        public String host;
        public String serviceName;
        public String serviceType;
        public String httpMethod;
        public String urlPath;
        public String resourceArn;
        public Map<String, String> attributes;

        public RuleParams(String ruleName) {
            name = ruleName;
        }
    }

    public static SamplingRule createRule(RuleParams params) {
        return SamplingRule.create(params.attributes, params.fixedRate,
                params.host, params.httpMethod, params.priority, params.reservoirSize, params.resourceArn, null,
                params.name, params.serviceName, params.serviceType, params.urlPath, null);
    }
}
