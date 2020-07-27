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

import com.amazonaws.xray.strategy.sampling.reservoir.Reservoir;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CentralizedReservoirTest {

    private static final int TEST_TIME = 1500;

    private static final int INTERVAL = 100;

    private int takeOverTime(Reservoir reservoir, int millis) {
        int numTaken = 0;
        for (int i = 0; i < millis / INTERVAL; i++) {
            if (reservoir.take()) {
                numTaken++;
            }
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException ie) {
                break;
            }
        }

        return numTaken;
    }

    @Test
    void testOnePerSecond() {
        int perSecond = 1;
        int taken = takeOverTime(new Reservoir(perSecond), TEST_TIME);
        Assertions.assertTrue(Math.ceil(TEST_TIME / 1000f) <= taken);
        Assertions.assertTrue(Math.ceil(TEST_TIME / 1000f) + perSecond >= taken);
    }

    @Test
    void testTenPerSecond() {
        int perSecond = 10;
        int taken = takeOverTime(new Reservoir(perSecond), TEST_TIME);
        Assertions.assertTrue(Math.ceil(TEST_TIME * perSecond / 1000f) <= taken);
        Assertions.assertTrue(Math.ceil(TEST_TIME * perSecond / 1000f) + perSecond >= taken);
    }
}
