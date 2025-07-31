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

package com.amazonaws.xray.strategy.sampling.reservoir;

import com.amazonaws.xray.strategy.sampling.GetSamplingRulesResponse.SamplingRule;
import com.amazonaws.xray.strategy.sampling.GetSamplingTargetsResponse.SamplingTargetDocument;
import java.time.Instant;

public class CentralizedReservoir {
    private static final long DEFAULT_INTERVAL = 10; // Seconds

    private long capacity;
    private long quota;
    private long used;

    private long currentEpoch;
    private long interval;
    private boolean borrow;
    private Instant refreshedAt;
    private Instant expiresAt;

    public CentralizedReservoir(long capacity) {
        this.capacity = capacity;
        this.expiresAt = Instant.EPOCH;
        this.refreshedAt = Instant.EPOCH;
        this.interval = DEFAULT_INTERVAL;
    }

    public void update(SamplingRule r) {
        capacity = r.getReservoirSize();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isBorrow(Instant now) {
        if (now.getEpochSecond() != currentEpoch) {
            reset(now);
        }
        boolean b = borrow;
        borrow = true;
        return !b && capacity != 0;
    }

    public boolean isStale(Instant now) {
        return now.isAfter(refreshedAt.plusSeconds(interval));
    }

    public void update(SamplingTargetDocument target, Instant now) {
        if (target.getReservoirQuota() != null) {
            quota = target.getReservoirQuota();
        }

        if (target.getReservoirQuotaTtl() != null) {
            expiresAt = target.getReservoirQuotaTtl().toInstant();
        }

        if (target.getIntervalSecs() != null) {
            interval = target.getIntervalSecs();
        }

        refreshedAt = now;
    }

    public boolean take(Instant now) {
        // We have moved to a new epoch. Reset reservoir.
        if (now.getEpochSecond() != currentEpoch) {
            reset(now);
        }

        if (quota > used) {
            used++;

            return true;
        }

        return false;
    }

    void reset(Instant now) {
        currentEpoch = now.getEpochSecond();
        used = 0;
        borrow = false;
    }

    public long getQuota() {
        return quota;
    }

    public long getUsed() {
        return used;
    }

    public long getCurrentEpoch() {
        return currentEpoch;
    }

    public long getInterval() {
        return interval;
    }

}
