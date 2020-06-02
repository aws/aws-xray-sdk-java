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

import com.amazonaws.xray.strategy.sampling.rule.SamplingRule;
import java.net.URL;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.powermock.reflect.Whitebox;

@FixMethodOrder(MethodSorters.JVM)
public class LocalizedSamplingStrategyTest {

    @Test
    public void testLocalizedSamplingStrategyWithDefaultRules() {
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy();
        assertDefaultRulesSet(localizedSamplingStrategy);
    }

    @Test
    public void testLocalizedSamplingStrategyWithInvalidURL() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("DoesntExist.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        assertDefaultRulesSet(localizedSamplingStrategy);
    }

    private void assertDefaultRulesSet(LocalizedSamplingStrategy localizedSamplingStrategy) {
        @SuppressWarnings("unchecked")
        ArrayList<SamplingRule> internalRules = (ArrayList<SamplingRule>) Whitebox.getInternalState(localizedSamplingStrategy,
                                                                                                    "rules");
        SamplingRule defaultRule = (SamplingRule) Whitebox.getInternalState(localizedSamplingStrategy, "defaultRule");

        Assert.assertEquals(0, internalRules.size());

        Assert.assertNull(defaultRule.getServiceName());
        Assert.assertNull(defaultRule.getHttpMethod());
        Assert.assertNull(defaultRule.getUrlPath());
        Assert.assertEquals(1, defaultRule.getFixedTarget());
        Assert.assertEquals(0.05, defaultRule.getRate(), 0.0000001);
    }

    @Test(expected = RuntimeException.class)
    public void testLocalizedSamplingStrategyWithoutDefaultRuleThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/EmptySamplingRules.json");
        new LocalizedSamplingStrategy(emptySamplingRules);
    }

    @Test(expected = RuntimeException.class)
    public void testLocalizedSamplingStrategyWithExtraAttributesOnDefaultRuleThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/ExtraAttributesOnDefaultSamplingRules.json");
        new LocalizedSamplingStrategy(emptySamplingRules);
    }

    @Test(expected = RuntimeException.class)
    public void testLocalizedSamplingStrategyWithMissingAttributesThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/MissingAttributesSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        SamplingRequest samplingRequest = new SamplingRequest("", "test", "/test", "test", "");
        SamplingResponse sr = localizedSamplingStrategy.shouldTrace(samplingRequest);
        Assert.assertFalse(sr.isSampled());
    }

    @Test(expected = RuntimeException.class)
    public void testLocalizedSamplingStrategyWithOneRuleMissingAttributesThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/OneRuleMissingAttributesSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        SamplingRequest samplingRequest = new SamplingRequest("", "test", "/test", "test", "");
        SamplingResponse sr = localizedSamplingStrategy.shouldTrace(samplingRequest);
        Assert.assertFalse(sr.isSampled());
    }

    @Test
    public void testLocalizedSamplingStrategyWithTwoRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/TwoSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        SamplingRequest r1 = new SamplingRequest("", "test", "/test", "test", "");
        SamplingResponse s1 = localizedSamplingStrategy.shouldTrace(r1);
        Assert.assertTrue(s1.isSampled());
        SamplingRequest r2 = new SamplingRequest("", "test", "/no", "test", "");
        SamplingResponse s2 = localizedSamplingStrategy.shouldTrace(r2);
        Assert.assertFalse(s2.isSampled());
    }

    @Test
    public void testLocalizedSamplingStrategyWithThreeRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/ThreeSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        SamplingRequest r1 = new SamplingRequest("", "test", "/test", "test", "");
        SamplingResponse s1 = localizedSamplingStrategy.shouldTrace(r1);
        Assert.assertFalse(s1.isSampled());
        SamplingRequest r2 = new SamplingRequest("", "test2", "/test", "test", "");
        SamplingResponse s2 = localizedSamplingStrategy.shouldTrace(r2);
        Assert.assertTrue(s2.isSampled());
        SamplingRequest r3 = new SamplingRequest("", "no", "/test", "test", "");
        SamplingResponse s3 = localizedSamplingStrategy.shouldTrace(r3);
        Assert.assertFalse(s3.isSampled());
    }

    @Test
    public void testLocalizedSamplingStrategyWithFourRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/FourSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        SamplingRequest r1 = new SamplingRequest("", "test", "/test", "test", "");
        SamplingResponse s1 = localizedSamplingStrategy.shouldTrace(r1);
        Assert.assertFalse(s1.isSampled());
        SamplingRequest r2 = new SamplingRequest("", "test", "/test", "rest", "");
        SamplingResponse s2 = localizedSamplingStrategy.shouldTrace(r2);
        Assert.assertTrue(s2.isSampled());
        SamplingRequest r3 = new SamplingRequest("", "test", "/test", "no", "");
        SamplingResponse s3 = localizedSamplingStrategy.shouldTrace(r3);
        Assert.assertFalse(s3.isSampled());
    }

    @Test
    public void testSamplingRequestHasNullField() {
        URL samplingRules = LocalizedSamplingStrategyTest.class.getResource(
            "/com/amazonaws/xray/strategy/sampling/TwoSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(samplingRules);
        localizedSamplingStrategy.shouldTrace(new SamplingRequest(null, null, null, null, ""));
    }

}
