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
import com.amazonaws.xray.strategy.sampling.SamplingRequest;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import com.amazonaws.xray.strategy.sampling.rule.CentralizedRule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

class CentralizedManifestTest {

    @Test
    void testEmptyManifestSize() {
        CentralizedManifest manifest = new CentralizedManifest();
        Assertions.assertEquals(0, manifest.size());
    }

    @Test
    void testExpirationForNewManifest() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();
        Assertions.assertTrue(manifest.isExpired(now));
    }

    @Test
    void testExpirationForNewlyRefreshedManifest() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();
        SamplingRule r1 = rule("r1");
        manifest.putRules(Arrays.asList(r1), now);

        Assertions.assertFalse(manifest.isExpired(now));
    }

    @Test
    void testExpirationForOldManifest() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();
        SamplingRule r1 = rule("r1");
        manifest.putRules(Arrays.asList(r1), now);

        // Increment time to be one second past expiration
        now = Instant.ofEpochSecond(1500003601);

        Assertions.assertTrue(manifest.isExpired(now));
    }

    @Test
    void testPositiveMatch() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();

        SamplingRule r1 = new SamplingRule()
            .withRuleName("r1")
            .withPriority(10)
            .withReservoirSize(20)
            .withFixedRate(0.05)
            .withHost("*")
            .withServiceName("*")
            .withHTTPMethod("*")
            .withURLPath("*")
            .withResourceARN("*")
            .withServiceType("*");

        manifest.putRules(Arrays.asList(r1), now);

        SamplingRequest req = new SamplingRequest(
            "privileged",
            "resourceARN",
            "service",
            "host",
            "method",
            "url",
            "serviceType",
            null
        );

        Assertions.assertEquals("r1", manifest.match(req, now).sample(now).getRuleName().get());
    }

    @Test
    void testPositiveDefaultRuleMatch() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();

        SamplingRule r2 = new SamplingRule()
            .withRuleName(CentralizedRule.DEFAULT_RULE_NAME)
            .withReservoirSize(20)
            .withFixedRate(0.05);

        manifest.putRules(Arrays.asList(rule("r1"), r2), now);

        // Request that matches against the default rule
        SamplingRequest req = new SamplingRequest(
            "privileged",
            "resourceARN",
            "service",
            "host",
            "method",
            "url",
            "serviceType",
            null
        );

        Assertions.assertEquals(CentralizedRule.DEFAULT_RULE_NAME, manifest.match(req, now).sample(now).getRuleName().get());
    }

    @Test
    void testPutRules() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest manifest = new CentralizedManifest();

        // Liberal sampling rule
        SamplingRule r1 = new SamplingRule()
            .withRuleName("r1")
            .withPriority(10)
            .withReservoirSize(20)
            .withFixedRate(0.05)
            .withHost("*")
            .withServiceName("*")
            .withHTTPMethod("*")
            .withURLPath("*")
            .withResourceARN("*")
            .withServiceType("*");

        manifest.putRules(Arrays.asList(r1), now);

        SamplingRequest req = new SamplingRequest(
            "privileged",
            "resourceARN",
            "service",
            "host",
            "method",
            "url",
            "serviceType",
            null
        );

        Assertions.assertEquals("r1", manifest.match(req, now).sample(now).getRuleName().get());
    }

    @Test
    void testRebuildOnNewRule() {
        CentralizedManifest manifest = new CentralizedManifest();

        manifest.putRules(Arrays.asList(rule("r1")), Instant.now());
        Map<String, CentralizedRule> rules1 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        manifest.putRules(Arrays.asList(rule("r1"), rule("r2")), Instant.now());
        Map<String, CentralizedRule> rules2 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        // The map of rules should be rebuilt, resulting in a new object
        Assertions.assertFalse(rules1 == rules2);

        Assertions.assertEquals(1, rules1.size());
        Assertions.assertEquals(2, rules2.size());
    }

    @Test
    void testPutRulesWithoutRebuild() {
        CentralizedManifest manifest = new CentralizedManifest();

        manifest.putRules(Arrays.asList(rule("r1")), Instant.now());
        Map<String, CentralizedRule> rules1 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        SamplingRule r = rule("r1").withResourceARN("arn3");

        manifest.putRules(Arrays.asList(r), Instant.now());
        Map<String, CentralizedRule> rules2 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        // The map of rules should not have been rebuilt
        Assertions.assertTrue(rules1 == rules2);
    }

    @Test
    void testRebuildOnPriorityChange() {
        CentralizedManifest manifest = new CentralizedManifest();

        manifest.putRules(Arrays.asList(rule("r1")), Instant.now());
        Map<String, CentralizedRule> rules1 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        SamplingRule r = rule("r1").withPriority(200);

        manifest.putRules(Arrays.asList(r), Instant.now());
        Map<String, CentralizedRule> rules2 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        // The map of rules should be rebuilt, resulting in a new object
        Assertions.assertFalse(rules1 == rules2);

        Assertions.assertEquals(1, rules1.size());
        Assertions.assertEquals(1, rules2.size());
    }

    @Test
    void testRebuildOnRuleDeletion() {
        CentralizedManifest manifest = new CentralizedManifest();

        manifest.putRules(Arrays.asList(rule("r1"), rule("r2")), Instant.now());
        Map<String, CentralizedRule> rules1 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        manifest.putRules(Arrays.asList(rule("r2")), Instant.now());
        Map<String, CentralizedRule> rules2 = Whitebox.getInternalState(manifest, "rules", CentralizedManifest.class);

        // The map of rules should be rebuilt, resulting in a new object
        Assertions.assertFalse(rules1 == rules2);

        Assertions.assertEquals(2, rules1.size());
        Assertions.assertEquals(1, rules2.size());
    }

    @Test
    void testManifestSizeWithDefaultRule() {
        CentralizedManifest m = new CentralizedManifest();

        SamplingRule r2 = new SamplingRule()
            .withRuleName(CentralizedRule.DEFAULT_RULE_NAME)
            .withReservoirSize(20)
            .withFixedRate(0.05);

        m.putRules(Arrays.asList(rule("r1"), r2), Instant.now());

        Assertions.assertEquals(2, m.size());
    }

    @Test
    void testManifestSizeWithoutDefaultRule() {
        CentralizedManifest m = new CentralizedManifest();

        SamplingRule r1 = new SamplingRule()
            .withRuleName(CentralizedRule.DEFAULT_RULE_NAME)
            .withReservoirSize(20)
            .withFixedRate(0.05);

        m.putRules(Arrays.asList(r1), Instant.now());

        Assertions.assertEquals(1, m.size());
    }

    @Test
    void testSnapshotsWithDefaultRule() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest m = new CentralizedManifest();
        m.putRules(Arrays.asList(
            rule("r1"),
            rule("r2"),
            rule(CentralizedRule.DEFAULT_RULE_NAME)
        ), now);

        Map<String, CentralizedRule> rules = Whitebox.getInternalState(m, "rules", CentralizedManifest.class);
        CentralizedRule defaultRule = Whitebox.getInternalState(m, "defaultRule", CentralizedManifest.class);

        rules.forEach((key, r) -> r.sample(now));
        defaultRule.sample(now);

        List<SamplingStatisticsDocument> snapshots = m.snapshots(now);

        Assertions.assertEquals(3, snapshots.size());
    }

    @Test
    void testSnapshotsWithoutDefaultRule() {
        Instant now = Instant.ofEpochSecond(1500000000);

        CentralizedManifest m = new CentralizedManifest();
        m.putRules(Arrays.asList(
            rule("r1"),
            rule("r2")
        ), now);

        Map<String, CentralizedRule> rules = Whitebox.getInternalState(m, "rules", CentralizedManifest.class);
        rules.forEach((key, r) -> r.sample(now));

        List<SamplingStatisticsDocument> snapshots = m.snapshots(now);

        Assertions.assertEquals(2, snapshots.size());
    }

    @Test
    void testRebuild() {
        Map<String, CentralizedRule> rules = new HashMap<>();
        rules.put("r1", new CentralizedRule(rule("r1").withPriority(11), new RandImpl()));
        rules.put("r2", new CentralizedRule(rule("r2"), new RandImpl()));

        List<SamplingRule> inputs = new ArrayList<>();
        inputs.add(rule("r2"));
        inputs.add(rule("r1"));
        inputs.add(rule("r3"));

        CentralizedManifest m = new CentralizedManifest();
        Map<String, CentralizedRule> rebuiltRules = m.rebuild(rules, inputs);

        Assertions.assertEquals(3, rebuiltRules.size());

        String[] orderedList = new String[3];
        rebuiltRules.keySet().toArray(orderedList);

        Assertions.assertEquals("r2", orderedList[0]);
        Assertions.assertEquals("r3", orderedList[1]);
        Assertions.assertEquals("r1", orderedList[2]);
    }

    private SamplingRule rule(String ruleName) {
        SamplingRule r = new SamplingRule()
            .withRuleName(ruleName)
            .withPriority(10)
            .withReservoirSize(20)
            .withFixedRate(0.05)
            .withHost("*")
            .withServiceName("s2")
            .withHTTPMethod("POST")
            .withURLPath("/foo")
            .withResourceARN("arn2");

        return r;
    }

}
