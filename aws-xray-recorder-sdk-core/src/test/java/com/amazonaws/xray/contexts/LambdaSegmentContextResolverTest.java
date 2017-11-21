package com.amazonaws.xray.contexts;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@FixMethodOrder(MethodSorters.JVM)
@PrepareForTest(LambdaSegmentContextResolver.class)
@RunWith(PowerMockRunner.class)
public class LambdaSegmentContextResolverTest {

    @Test
    public void testLambdaTaskRootSetResolvesLambdaContext() {
        Assert.assertNotNull(mockResolver("/var/test").resolve());
    }

    @Test
    public void testBlankLambdaTaskRootDoesNotResolveLambdaContext() {
        Assert.assertNull(mockResolver(" ").resolve());
    }

    @Test
    public void testLambdaTaskRootNotSetDoesNotResolveLambdaContext() {
        Assert.assertNull(mockResolver(null).resolve());
    }

    private LambdaSegmentContextResolver mockResolver(String taskRoot) {
        PowerMockito.stub(PowerMockito.method(LambdaSegmentContextResolver.class, "getLambdaTaskRoot")).toReturn(taskRoot);
        return new LambdaSegmentContextResolver();
    }
}
