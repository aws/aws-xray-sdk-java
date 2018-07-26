package com.amazonaws.xray.strategy.sampling;

import com.amazonaws.xray.strategy.sampling.reservoir.Reservoir;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class CentralizedReservoirTest {

    private static final int INTERVAL = 100;
    private int takeOverTime(Reservoir reservoir, int millis) {
        int numTaken = 0;
        for (int i = 0; i < millis/INTERVAL; i++) {
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


    private static final int TEST_TIME = 1500;
    @Test
    public void testOnePerSecond() {
        int perSecond = 1;
        int taken = takeOverTime(new Reservoir(perSecond), TEST_TIME);
        Assert.assertTrue(Math.ceil(TEST_TIME/1000f) <= taken);
        Assert.assertTrue(Math.ceil(TEST_TIME/1000f) + perSecond >= taken);
    }

    @Test
    public void testTenPerSecond() {
        int perSecond = 10;
        int taken = takeOverTime(new Reservoir(perSecond), TEST_TIME);
        Assert.assertTrue(Math.ceil(TEST_TIME*perSecond/1000f) <= taken);
        Assert.assertTrue(Math.ceil(TEST_TIME*perSecond/1000f) + perSecond >= taken);
    }
}
