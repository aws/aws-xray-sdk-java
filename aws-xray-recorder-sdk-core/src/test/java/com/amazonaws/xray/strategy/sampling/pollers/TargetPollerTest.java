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

package com.amazonaws.xray.strategy.sampling.pollers;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.xray.internal.UnsignedXrayClient;
import com.amazonaws.xray.strategy.sampling.manifest.CentralizedManifest;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TargetPollerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    private CentralizedManifest manifest;

    @Mock
    private UnsignedXrayClient client;

    @Test
    public void testPollerShutdown() {
        TargetPoller poller = new TargetPoller(client, manifest, Clock.systemUTC());
        poller.start();
        poller.shutdown();

        assertThat(poller.getExecutor().isShutdown()).isTrue();
    }

    @Test
    public void testInterval() throws Exception {
        TargetPoller poller = new TargetPoller(client, manifest, Clock.systemUTC());

        Method method = TargetPoller.class.getDeclaredMethod("getIntervalWithJitter");
        method.setAccessible(true);

        boolean nineSecondBucket = false;
        boolean tenSecondBucket = false;
        boolean elevenSecondBucket = false;

        for (int i = 0; i < 1000; i++) {
            long interval = (Long) method.invoke(poller);

            if (interval / 1000 == 9) {
                nineSecondBucket = true;
            } else if (interval / 1000 == 10) {
                tenSecondBucket = true;
            } else if (interval / 1000 == 11) {
                elevenSecondBucket = true;
            }

            assertThat(interval).isLessThanOrEqualTo(TimeUnit.SECONDS.toMillis(12));
            assertThat(interval).isGreaterThanOrEqualTo(TimeUnit.SECONDS.toMillis(9));
        }

        assertThat(nineSecondBucket).isTrue();
        assertThat(tenSecondBucket).isTrue();
        assertThat(elevenSecondBucket).isTrue();
    }
}
