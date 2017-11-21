package com.amazonaws.xray.strategy.sampling;

import java.net.URL;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.internal.util.reflection.Whitebox;

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
        ArrayList<SamplingRule> internalRules = (ArrayList<SamplingRule>) Whitebox.getInternalState(localizedSamplingStrategy, "rules");
        SamplingRule defaultRule = (SamplingRule) Whitebox.getInternalState(localizedSamplingStrategy, "defaultRule");

        Assert.assertEquals(0, internalRules.size());

        Assert.assertNull(defaultRule.getServiceName());
        Assert.assertNull(defaultRule.getHttpMethod());
        Assert.assertNull(defaultRule.getUrlPath());
        Assert.assertEquals(1, defaultRule.getFixedTarget());
        Assert.assertEquals(0.05, defaultRule.getRate(), 0.0000001);
    }

    @Test(expected=RuntimeException.class)
    public void testLocalizedSamplingStrategyWithoutDefaultRuleThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/EmptySamplingRules.json");
        new LocalizedSamplingStrategy(emptySamplingRules);
    }

    @Test(expected=RuntimeException.class)
    public void testLocalizedSamplingStrategyWithExtraAttributesOnDefaultRuleThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/ExtraAttributesOnDefaultSamplingRules.json");
        new LocalizedSamplingStrategy(emptySamplingRules);
    }

    @Test(expected=RuntimeException.class)
    public void testLocalizedSamplingStrategyWithMissingAttributesThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/MissingAttributesSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/test", "test"));
    }

    @Test(expected=RuntimeException.class)
    public void testLocalizedSamplingStrategyWithOneRuleMissingAttributesThrowsRuntimeException() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/OneRuleMissingAttributesSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/test", "test"));
    }

    @Test
    public void testLocalizedSamplingStrategyWithTwoRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/TwoSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);
        Assert.assertTrue(localizedSamplingStrategy.shouldTrace("test", "/test", "test"));
        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/no", "test"));
    }

    @Test
    public void testLocalizedSamplingStrategyWithThreeRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/ThreeSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);

        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/test", "test"));
        Assert.assertTrue(localizedSamplingStrategy.shouldTrace("test2", "/test", "test"));
        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("no", "/test", "test"));
    }

    @Test
    public void testLocalizedSamplingStrategyWithFourRules() {
        URL emptySamplingRules = LocalizedSamplingStrategyTest.class.getResource("/com/amazonaws/xray/strategy/sampling/FourSamplingRules.json");
        LocalizedSamplingStrategy localizedSamplingStrategy = new LocalizedSamplingStrategy(emptySamplingRules);

        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/test", "test"));
        Assert.assertTrue(localizedSamplingStrategy.shouldTrace("test", "/test", "rest"));
        Assert.assertFalse(localizedSamplingStrategy.shouldTrace("test", "/test", "no"));
    }

}
