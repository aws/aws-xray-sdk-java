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
import com.amazonaws.xray.strategy.sampling.GetSamplingTargetsRequest.SamplingStatisticsDocument;
import com.amazonaws.xray.strategy.sampling.GetSamplingTargetsResponse.SamplingTargetDocument;
import com.amazonaws.xray.strategy.sampling.SamplingResponse;
import com.amazonaws.xray.strategy.sampling.rand.Rand;
import com.amazonaws.xray.strategy.sampling.rand.RandImpl;
import com.amazonaws.xray.strategy.sampling.reservoir.CentralizedReservoir;
import com.amazonaws.xray.strategy.sampling.rule.RuleBuilder.RuleParams;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class CentralizedRuleTest {

    @Mock
    private Rand rand;

    @Mock
    private Clock clock;

    @Test
    public void testPositiveSampleTake() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule rule = new CentralizedRule(input, new RandImpl());

        SamplingTargetDocument target = createTarget(2, 0.0, 1500000010);
        rule.update(target, clock.instant());

        SamplingResponse response = rule.sample(clock.instant());

        Assert.assertTrue(response.isSampled());
        Assert.assertEquals("r1", response.getRuleName().get());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        Assert.assertEquals(1, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());
    }

    @Test
    public void testPositiveBernoulliSample() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule rule = new CentralizedRule(input, rand);

        Mockito.when(rand.next()).thenReturn(0.01);

        SamplingTargetDocument target = createTarget(0, 0.05, 1500000010);
        rule.update(target, clock.instant());

        // Sample using bernoulli sampling
        SamplingResponse response = rule.sample(clock.instant());

        Assert.assertTrue(response.isSampled());
        Assert.assertEquals("r1", response.getRuleName().get());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        Assert.assertEquals(1, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());
    }

    @Test
    public void testExpiredReservoirPositiveBernoulliSample() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 0, 0.5);
        CentralizedRule rule = new CentralizedRule(input, rand);

        SamplingTargetDocument target = createTarget(0, 0.5, 1499999999);
        rule.update(target, clock.instant());

        Mockito.when(rand.next()).thenReturn(0.2);

        // BernoulliSample() from expired reservoir
        SamplingResponse response = rule.sample(clock.instant());

        Mockito.verify(rand).next();
        Assert.assertTrue(response.isSampled());
        Assert.assertEquals("r1", response.getRuleName().get());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        Assert.assertEquals(1, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());
    }

    @Test
    public void testNegativeSample() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule rule = new CentralizedRule(input, rand);

        SamplingTargetDocument target = createTarget(0, 0.0, 1500000010);
        rule.update(target, clock.instant());

        SamplingResponse response = rule.sample(clock.instant());

        Assert.assertFalse(response.isSampled());
        Assert.assertEquals("r1", response.getRuleName().get());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        Assert.assertEquals(0, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());
    }

    @Test
    public void testExpiredReservoirNegativeBernoulliSample() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 0, 0.2);
        CentralizedRule rule = new CentralizedRule(input, rand);

        SamplingTargetDocument target = createTarget(0, 0.2, 1499999999);
        rule.update(target, clock.instant());

        Mockito.when(rand.next()).thenReturn(0.4);

        SamplingResponse response = rule.sample(clock.instant());

        Assert.assertFalse(response.isSampled());
        Assert.assertEquals("r1", response.getRuleName().get());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        Assert.assertEquals(0, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());
    }

    @Test
    public void testReservoirReset() {
        Mockito.when(clock.instant()).thenReturn(Instant.ofEpochSecond(1500000000));

        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule rule = new CentralizedRule(input, new RandImpl());

        SamplingTargetDocument target = createTarget(2, 0.0, 1500000010);
        rule.update(target, clock.instant());

        rule.sample(clock.instant());

        CentralizedReservoir reservoir = Whitebox.getInternalState(rule, "centralizedReservoir", CentralizedRule.class);

        Assert.assertEquals(1, reservoir.getUsed());

        Mockito.when(clock.instant()).thenReturn(Instant.ofEpochSecond(1500000001));

        rule.sample(clock.instant());

        // Assert to ensure reservoir was reset before being updated
        Assert.assertEquals(1, reservoir.getUsed());
    }

    @Test
    public void testSnapshot() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1500000000), ZoneId.systemDefault());

        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule rule = new CentralizedRule(input, new RandImpl());

        SamplingTargetDocument target = createTarget(2, 0.0, 1500000010);
        rule.update(target, clock.instant());

        rule.sample(clock.instant());

        Statistics s = Whitebox.getInternalState(rule, "statistics", CentralizedRule.class);

        // Assert statistics were updated
        Assert.assertEquals(1, s.getSampled());
        Assert.assertEquals(1, s.getRequests());
        Assert.assertEquals(0, s.getBorrowed());

        SamplingStatisticsDocument snapshot = rule.snapshot(Date.from(clock.instant()));

        // Assert snapshot contains expected statistics
        Assert.assertEquals("r1", snapshot.getRuleName());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(1500000000), snapshot.getTimestamp().toInstant().toEpochMilli());
        Assert.assertEquals(1, snapshot.getRequestCount());
        Assert.assertEquals(1, snapshot.getSampledCount());
        Assert.assertEquals(0, snapshot.getBorrowCount());

        // Assert current statistics are empty
        Assert.assertEquals(0, rule.snapshot(Date.from(clock.instant())).getRequestCount());
        Assert.assertEquals(0, rule.snapshot(Date.from(clock.instant())).getSampledCount());
        Assert.assertEquals(0, rule.snapshot(Date.from(clock.instant())).getBorrowCount());
    }

    @Test
    public void testRuleUpdateWithInvalidation() {
        SamplingRule input = createInput("r1", 300, 10, 0.0, "POST", "s1", "/foo/bar");
        CentralizedRule r = new CentralizedRule(input, new RandImpl());

        SamplingRule update = createInput("r1", 301, 5, 0.5, "GET", "s2", "/bar/foo");
        boolean invalidate = r.update(update);

        Matchers m = Whitebox.getInternalState(r, "matchers", CentralizedRule.class);

        Assert.assertEquals("GET", Whitebox.getInternalState(m, "method", Matchers.class));
        Assert.assertEquals("s2", Whitebox.getInternalState(m, "service", Matchers.class));
        Assert.assertEquals("/bar/foo", Whitebox.getInternalState(m, "url", Matchers.class));
        Assert.assertTrue(invalidate);
    }

    @Test
    public void testRuleUpdateWithoutInvalidation() {
        SamplingRule input = createInput("r1", 300, 10, 0.0, "POST", "s1", "/foo/bar");
        CentralizedRule r = new CentralizedRule(input, new RandImpl());

        SamplingRule update = createInput("r1", 300, 10, 0.0, "GET", "s2", "/bar/foo");
        boolean invalidate = r.update(update);

        Matchers m = Whitebox.getInternalState(r, "matchers", CentralizedRule.class);

        Assert.assertEquals("GET", Whitebox.getInternalState(m, "method", Matchers.class));
        Assert.assertEquals("s2", Whitebox.getInternalState(m, "service", Matchers.class));
        Assert.assertEquals("/bar/foo", Whitebox.getInternalState(m, "url", Matchers.class));
        Assert.assertFalse(invalidate);
    }

    @Test
    public void testTargetUpdate() {
        SamplingRule input = createInput("r1", 300, 10, 0.0);
        CentralizedRule r = new CentralizedRule(input, new RandImpl());

        SamplingTargetDocument update = SamplingTargetDocument.create(0.5,
            20, null, null, "r1");

        r.update(update, Instant.now());

        double fixedRate = Whitebox.getInternalState(r, "fixedRate", CentralizedRule.class);

        Assert.assertEquals(0.5, fixedRate, 0);
    }

    public static SamplingRule createInput(String name, int priority, int capacity, double rate) {
        RuleParams params = new RuleParams(name);
        params.priority = priority;
        params.reservoirSize = capacity;
        params.fixedRate = rate;
        return RuleBuilder.createRule(params);
    }

    public static SamplingRule createInput(String name, int priority, int capacity, double rate,
        String httpMethod, String serviceName, String urlPath) {
        RuleParams params = new RuleParams(name);
        params.priority = priority;
        params.reservoirSize = capacity;
        params.fixedRate = rate;
        params.httpMethod = httpMethod;
        params.serviceName = serviceName;
        params.urlPath = urlPath;
        return RuleBuilder.createRule(params);
    }

    public static SamplingTargetDocument createTarget(int quota, double rate, long expiresAt) {
        SamplingTargetDocument target = SamplingTargetDocument.create(
            rate,
            10,
            quota,
            Date.from(Instant.ofEpochSecond(expiresAt)),
            "");

        return target;
    }
}
