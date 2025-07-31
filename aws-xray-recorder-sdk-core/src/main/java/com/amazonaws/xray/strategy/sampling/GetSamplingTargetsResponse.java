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
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class GetSamplingTargetsResponse {

    @JsonCreator
    static GetSamplingTargetsResponse create(
            @JsonProperty("LastRuleModification") Date lastRuleModification,
            @JsonProperty("SamplingTargetDocuments") List<SamplingTargetDocument> documents,
            @JsonProperty("UnprocessedStatistics") List<UnprocessedStatistics> unprocessedStatistics) {
        return new AutoValue_GetSamplingTargetsResponse(
                lastRuleModification, documents, unprocessedStatistics);
    }

    public abstract Date getLastRuleModification();

    public abstract List<SamplingTargetDocument> getDocuments();

    public abstract List<UnprocessedStatistics> getUnprocessedStatistics();

    @AutoValue
    public abstract static class SamplingTargetDocument {

        @JsonCreator
        public static SamplingTargetDocument create(
                @JsonProperty("FixedRate") double fixedRate,
                @JsonProperty("Interval") @Nullable Integer intervalSecs,
                @JsonProperty("ReservoirQuota") @Nullable Integer reservoirQuota,
                @JsonProperty("ReservoirQuotaTTL") @Nullable Date reservoirQuotaTtl,
                @JsonProperty("RuleName") String ruleName) {
            return new AutoValue_GetSamplingTargetsResponse_SamplingTargetDocument(
                    fixedRate, intervalSecs, reservoirQuota, reservoirQuotaTtl, ruleName);
        }

        public abstract double getFixedRate();

        @Nullable
        public abstract Integer getIntervalSecs();

        @Nullable
        public abstract Integer getReservoirQuota();

        // Careful that this is a timestamp when the quota expires, not a duration as we'd normally
        // expect for a Time to live.
        @Nullable
        public abstract Date getReservoirQuotaTtl();

        public abstract String getRuleName();
    }

    @AutoValue
    abstract static class UnprocessedStatistics {

        @JsonCreator
        static UnprocessedStatistics create(
                @JsonProperty("ErrorCode") String errorCode,
                @JsonProperty("Message") String message,
                @JsonProperty("RuleName") String ruleName) {
            return new AutoValue_GetSamplingTargetsResponse_UnprocessedStatistics(
                    errorCode, message, ruleName);
        }

        public abstract String getErrorCode();

        public abstract String getMessage();

        public abstract String getRuleName();
    }
}
