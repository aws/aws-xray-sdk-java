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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class GetSamplingRulesResponse {

    @JsonCreator
    static GetSamplingRulesResponse create(
            @JsonProperty("NextToken") String nextToken,
            @JsonProperty("SamplingRuleRecords") List<SamplingRuleRecord> samplingRuleRecords) {
        return new AutoValue_GetSamplingRulesResponse(nextToken, samplingRuleRecords);
    }

    @Nullable
    abstract String getNextToken();

    public abstract List<SamplingRuleRecord> getSamplingRuleRecords();

    @AutoValue
    public abstract static class SamplingRuleRecord {

        @JsonCreator
        static SamplingRuleRecord create(
                @JsonProperty("CreatedAt") String createdAt,
                @JsonProperty("ModifiedAt") String modifiedAt,
                @JsonProperty("SamplingRule") SamplingRule samplingRule) {
            return new AutoValue_GetSamplingRulesResponse_SamplingRuleRecord(createdAt, modifiedAt, samplingRule);
        }

        public abstract String getCreatedAt();

        public abstract String getModifiedAt();

        public abstract SamplingRule getSamplingRule();
    }

    @AutoValue
    public abstract static class SamplingRule {

        @JsonCreator
        public static SamplingRule create(
                @JsonProperty("Attributes") Map<String, String> attributes,
                @JsonProperty("FixedRate") Double fixedRate,
                @JsonProperty("Host") String host,
                @JsonProperty("HTTPMethod") String httpMethod,
                @JsonProperty("Priority") Integer priority,
                @JsonProperty("ReservoirSize") Integer reservoirSize,
                @JsonProperty("ResourceARN") String resourceArn,
                @JsonProperty("RuleARN") @Nullable String ruleArn,
                @JsonProperty("RuleName") @Nullable String ruleName,
                @JsonProperty("ServiceName") String serviceName,
                @JsonProperty("ServiceType") String serviceType,
                @JsonProperty("URLPath") String urlPath,
                @JsonProperty("Version") Integer version) {
            return new AutoValue_GetSamplingRulesResponse_SamplingRule(
                    attributes,
                    fixedRate,
                    host,
                    httpMethod,
                    priority,
                    reservoirSize,
                    resourceArn,
                    ruleArn,
                    ruleName,
                    serviceName,
                    serviceType,
                    urlPath,
                    version);
        }

        @Nullable
        public abstract Map<String, String> getAttributes();

        public abstract Double getFixedRate();

        @Nullable
        public abstract String getHost();

        @Nullable
        public abstract String getHttpMethod();

        public abstract Integer getPriority();

        public abstract Integer getReservoirSize();

        @Nullable
        public abstract String getResourceArn();

        @Nullable
        public abstract String getRuleArn();

        @Nullable
        public abstract String getRuleName();

        @Nullable
        public abstract String getServiceName();

        @Nullable
        public abstract String getServiceType();

        @Nullable
        public abstract String getUrlPath();

        @Nullable
        public abstract Integer getVersion();
    }
}
