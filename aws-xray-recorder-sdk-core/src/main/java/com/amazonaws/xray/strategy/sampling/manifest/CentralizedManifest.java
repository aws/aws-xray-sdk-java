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

import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.services.xray.model.SamplingStatisticsDocument;
import com.amazonaws.services.xray.model.SamplingTargetDocument;
import com.amazonaws.xray.strategy.sampling.CentralizedSamplingStrategy;
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import com.amazonaws.xray.strategy.sampling.rule.Rule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CentralizedManifest implements Manifest {

    private static final long TTL = 3600; // Seconds

    // Map of customer-defined rules. Does not include customer default rule. Sorted by rule priority.
    private volatile LinkedHashMap<String, CentralizedRule> rules;

    // Customer default rule that matches against everything.
    @MonotonicNonNull
    private volatile CentralizedRule defaultRule;

    // Timestamp of last known valid refresh. Kept volatile for swapping with new timestamp on refresh.
    private volatile Instant refreshedAt;

    public CentralizedManifest() {
        this.rules = new LinkedHashMap<>(0);
        this.refreshedAt = Instant.EPOCH;
    }

    public LinkedHashMap<String, CentralizedRule> getRules() {
        return rules;
    }

    @Nullable
    public CentralizedRule getDefaultRule() {
        return defaultRule;
    }

    public boolean isExpired(Instant now) {
        return refreshedAt.plusSeconds(TTL).isBefore(now);
    }

    public int size() {
        if (defaultRule != null) {
            return rules.size() + 1;
        }

        return rules.size();
    }

    @Override
    // TODO(anuraaga): It seems like this should never return null, check where defaultRule is guaranteed to be present and remove
    @Nullable
    public Rule match(SamplingRequest req, Instant now) {
        for (CentralizedRule r : rules.values()) {
            if (!r.match(req)) {
                continue;
            }

            return r;
        }

        return defaultRule;
    }

    public void putRules(List<SamplingRule> inputs, Instant now) {
        // Set to true if we see a new or deleted rule or a change in the priority of an existing rule.
        boolean invalidate = false;

        Map<String, CentralizedRule> rules = this.rules;
        List<String> inputNames = new ArrayList<>(inputs.size());

        for (SamplingRule i : inputs) {
            if (i.getRuleName().equals(CentralizedRule.DEFAULT_RULE_NAME)) {
                putDefaultRule(i);
            } else {
                inputNames.add(i.getRuleName());
                invalidate = putCustomRule(rules, i);
            }
        }
        // Check if any rule was removed
        if (!invalidate) {
            for (CentralizedRule rule : rules.values()) {
                if (!inputNames.contains(rule.getName())) {
                    invalidate = true;
                    break;
                }
            }
        }

        if (invalidate) {
            this.rules = rebuild(rules, inputs);
        }

        this.refreshedAt = now;
    }

    public List<SamplingStatisticsDocument> snapshots(Instant now) {
        List<SamplingStatisticsDocument> snapshots = new ArrayList<>(rules.size() + 1);
        Date date = Date.from(now);

        for (CentralizedRule rule : rules.values()) {
            if (!rule.isStale(now)) {
                continue;
            }

            SamplingStatisticsDocument snapshot = rule.snapshot(date);
            snapshot.withClientID(CentralizedSamplingStrategy.getClientID());

            snapshots.add(snapshot);
        }

        if (defaultRule != null && defaultRule.isStale(now)) {
            SamplingStatisticsDocument snapshot = defaultRule.snapshot(date);
            snapshot.withClientID(CentralizedSamplingStrategy.getClientID());

            snapshots.add(snapshot);
        }

        return snapshots;
    }

    public void putTargets(List<SamplingTargetDocument> targets, Instant now) {
        Map<String, CentralizedRule> rules = this.rules;

        for (SamplingTargetDocument t : targets) {
            CentralizedRule r = null;

            if (rules.containsKey(t.getRuleName())) {
                r = rules.get(t.getRuleName());
            } else if (t.getRuleName().equals(CentralizedRule.DEFAULT_RULE_NAME)) {
                r = defaultRule;
            }

            if (r == null) {
                continue;
            }

            r.update(t, now);
        }
    }

    private boolean putCustomRule(Map<String, CentralizedRule> rules, SamplingRule i) {
        CentralizedRule r = rules.get(i.getRuleName());
        if (r == null) {
            return true;
        }

        return r.update(i);
    }

    private void putDefaultRule(SamplingRule i) {
        if (defaultRule == null) {
            defaultRule = new CentralizedRule(i, new RandImpl());
        } else {
            defaultRule.update(i);
        }
    }

    LinkedHashMap<String, CentralizedRule> rebuild(Map<String, CentralizedRule> old, List<SamplingRule> inputs) {
        List<CentralizedRule> rules = new ArrayList<>(inputs.size() - 1);

        for (SamplingRule i : inputs) {
            if (i.getRuleName().equals(CentralizedRule.DEFAULT_RULE_NAME)) {
                continue;
            }
            CentralizedRule r = old.get(i.getRuleName());
            if (r == null) {
                r = new CentralizedRule(i, new RandImpl());
            }

            rules.add(r);
        }
        Collections.sort(rules);

        LinkedHashMap<String, CentralizedRule> current = new LinkedHashMap<>(rules.size());
        for (CentralizedRule r: rules) {
            current.put(r.getName(), r);
        }

        return current;
    }
}
