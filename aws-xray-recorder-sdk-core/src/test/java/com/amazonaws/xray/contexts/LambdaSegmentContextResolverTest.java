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
