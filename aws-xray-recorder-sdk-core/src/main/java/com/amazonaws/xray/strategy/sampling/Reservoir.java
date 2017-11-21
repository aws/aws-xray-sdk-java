package com.amazonaws.xray.strategy.sampling;

import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

public class Reservoir {

    private int tracesPerSecond;

    private LongAdder usedThisSecond;
    private long thisSecond;

    public Reservoir() {
        this(0);
    }
    public Reservoir(int tracesPerSecond) {
        this.usedThisSecond = new LongAdder();
        this.tracesPerSecond = tracesPerSecond;
    }

    public boolean take() {
        long now = Instant.now().getEpochSecond();
        if (now != thisSecond) {
            usedThisSecond.reset();
            thisSecond = now;
        }
        if (usedThisSecond.intValue() >= tracesPerSecond) {
            return false;
        }
        usedThisSecond.increment();
        return true;
    }

    /**
     * @return the tracesPerSecond
     */
    public int getTracesPerSecond() {
        return tracesPerSecond;
    }
}
