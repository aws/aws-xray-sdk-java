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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
@JsonSerialize(as = GetSamplingTargetsRequest.class)
public abstract class GetSamplingTargetsRequest {

    public static GetSamplingTargetsRequest create(List<SamplingStatisticsDocument> documents) {
        return new AutoValue_GetSamplingTargetsRequest(documents);
    }

    // Limit of 25 items
    @JsonProperty("SamplingStatisticsDocuments")
    abstract List<SamplingStatisticsDocument> getDocuments();

    @AutoValue
    @JsonSerialize(as = SamplingStatisticsDocument.class)
    public abstract static class SamplingStatisticsDocument {

        public static SamplingStatisticsDocument.Builder newBuilder() {
            return new AutoValue_GetSamplingTargetsRequest_SamplingStatisticsDocument.Builder();
        }

        @JsonProperty("BorrowCount")
        public abstract long getBorrowCount();

        @Nullable
        @JsonProperty("ClientID")
        public abstract String getClientId();

        @JsonProperty("RequestCount")
        public abstract long getRequestCount();

        @JsonProperty("RuleName")
        public abstract String getRuleName();

        @JsonProperty("SampledCount")
        public abstract long getSampledCount();

        @JsonProperty("Timestamp")
        public abstract Date getTimestamp();

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder setBorrowCount(long borrowCount);

            public abstract Builder setClientId(String clientId);

            public abstract Builder setRequestCount(long requestCount);

            public abstract Builder setRuleName(String ruleName);

            public abstract Builder setSampledCount(long sampledCount);

            public abstract Builder setTimestamp(Date timestamp);

            public abstract SamplingStatisticsDocument build();
        }
    }
}
