package com.amazonaws.xray.strategy.sampling.rand;

import java.util.Random;

public class RandImpl implements Rand {

    private Random rand;

    public RandImpl() {
        this.rand = new Random(System.nanoTime());
    }

    @Override
    public double next() {
        return rand.nextDouble();
    }

}
