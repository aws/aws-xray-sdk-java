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

import static com.amazonaws.xray.strategy.sampling.reservoir.Reservoir.NANOS_PER_DECISECOND;
import static com.amazonaws.xray.strategy.sampling.reservoir.Reservoir.NANOS_PER_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Random;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
// Added to declutter console: tells power mock not to mess with implicit classes we aren't testing
@PowerMockIgnore({"org.apache.logging.*", "javax.script.*"})
@PrepareForTest(Reservoir.class)
public class ReservoirTest {

    @DataPoints public static final int[] SAMPLE_RESERVOIRS = {1, 10, 100};

    @Test public void samplesOnlySpecifiedNumber() {
        mockStatic(System.class);
        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND);
        Reservoir reservoir = new Reservoir(2);

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + 1);
        assertTrue(reservoir.take());
        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + 2);
        assertTrue(reservoir.take());
        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + 2);
        assertFalse(reservoir.take());
    }

    @Test public void samplesFairNegativeNanoTime() {
        mockStatic(System.class);
        when(System.nanoTime()).thenReturn(-2 * NANOS_PER_SECOND);
        Reservoir reservoir = new Reservoir(10);

        when(System.nanoTime()).thenReturn(-2 * NANOS_PER_SECOND + 1);
        assertTrue(reservoir.take());
        when(System.nanoTime()).thenReturn(-2 * NANOS_PER_SECOND + 2);
        assertTrue(reservoir.take());
        when(System.nanoTime()).thenReturn(-2 * NANOS_PER_SECOND + 2);
        assertFalse(reservoir.take());
    }

    @Test public void resetsAfterASecond() {
        mockStatic(System.class);

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND);
        Reservoir reservoir = new Reservoir(10);
        assertTrue(reservoir.take());
        assertFalse(reservoir.take());

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + NANOS_PER_DECISECOND);
        assertTrue(reservoir.take());
        assertFalse(reservoir.take());

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + NANOS_PER_DECISECOND * 9);
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertTrue(reservoir.take());
        assertFalse(reservoir.take());

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + NANOS_PER_SECOND);
        assertTrue(reservoir.take());
    }

    @Test public void allowsOddRates() {
        mockStatic(System.class);

        Reservoir reservoir = new Reservoir(11);
        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND);
        assertTrue(reservoir.take());

        when(System.nanoTime()).thenReturn(NANOS_PER_SECOND + NANOS_PER_DECISECOND * 9);
        for (int i = 1; i < 11; i++) {
            assertTrue("failed after " + (i + 1), reservoir.take());
        }
        assertFalse(reservoir.take());
    }

    @Test public void worksOnRollover() {
        mockStatic(System.class);
        when(System.nanoTime()).thenReturn(-NANOS_PER_SECOND);
        Reservoir reservoir = new Reservoir(2);
        assertTrue(reservoir.take());

        when(System.nanoTime()).thenReturn(-NANOS_PER_SECOND / 2);
        assertTrue(reservoir.take()); // second request

        when(System.nanoTime()).thenReturn(-NANOS_PER_SECOND / 4);
        assertFalse(reservoir.take());

        when(System.nanoTime()).thenReturn(0L); // reset
        assertTrue(reservoir.take());
    }

    @Theory public void retainsPerRate(int rate) {
        Reservoir reservoir = new Reservoir(rate);

        // parallel to ensure there aren't any unsynchronized race conditions
        long passed = new Random().longs(100000).parallel()
            .filter(i -> reservoir.take()).count();

        assertEquals(rate, passed);
    }
}
