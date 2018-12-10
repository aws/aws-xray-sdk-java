package com.amazonaws.xray.strategy.sampling.reservoir;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Reservoir {
    static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
    static final int NANOS_PER_DECISECOND = (int) (NANOS_PER_SECOND / 10);

    private final int tracesPerSecond;
    private final MaxFunction maxFunction;
    private final AtomicInteger usage = new AtomicInteger(0);
    private final AtomicLong nextReset;

    public Reservoir() {
        this(0);
    }

    public Reservoir(int tracesPerSecond) {
        this.tracesPerSecond = tracesPerSecond;
        this.maxFunction =
            tracesPerSecond < 10 ? new LessThan10(tracesPerSecond) : new AtLeast10(tracesPerSecond);
        long now = System.nanoTime();
        this.nextReset = new AtomicLong(now + NANOS_PER_SECOND);
    }

    public boolean take() {
        long now = System.nanoTime();
        long updateAt = nextReset.get();

        long nanosUntilReset = -(now - updateAt); // because nanoTime can be negative
        boolean shouldReset = nanosUntilReset <= 0;
        if (shouldReset) {
            if (nextReset.compareAndSet(updateAt, updateAt + NANOS_PER_SECOND)) {
                usage.set(0);
            }
        }

        int max = maxFunction.max(shouldReset ? 0 : nanosUntilReset);
        int prev, next;
        do { // same form as java 8 AtomicLong.getAndUpdate
            prev = usage.get();
            next = prev + 1;
            if (next > max) return false;
        } while (!usage.compareAndSet(prev, next));
        return true;
    }

    /**
     * @return the tracesPerSecond
     */
    public int getTracesPerSecond() {
        return tracesPerSecond;
    }

    static abstract class MaxFunction {
        /** @param nanosUntilReset zero if was just reset */
        abstract int max(long nanosUntilReset);
    }

    /** For a reservoir of less than 10, we permit draining it completely at any time in the second */
    static final class LessThan10 extends MaxFunction {
        final int tracesPerSecond;

        LessThan10(int tracesPerSecond) {
            this.tracesPerSecond = tracesPerSecond;
        }

        @Override int max(long nanosUntilReset) {
            return tracesPerSecond;
        }
    }

    /**
     * For a reservoir of at least 10, we permit draining up to a decisecond watermark. Because the
     * rate could be odd, we may have a remainder, which is arbitrarily available. We allow any
     * remainders in the 1st decisecond or any time thereafter.
     *
     * <p>Ex. If the rate is 10/s then you can use 1 in the first decisecond, another 1 in the 2nd,
     * or up to 10 by the last.
     *
     * <p>Ex. If the rate is 103/s then you can use 13 in the first decisecond, another 10 in the
     * 2nd, or up to 103 by the last.
     */
    static final class AtLeast10 extends MaxFunction {
        final int[] max;

        AtLeast10(int tracesPerSecond) {
            int tracesPerDecisecond = tracesPerSecond / 10, remainder = tracesPerSecond % 10;
            max = new int[10];
            max[0] = tracesPerDecisecond + remainder;
            for (int i = 1; i < 10; i++) {
                max[i] = max[i - 1] + tracesPerDecisecond;
            }
        }

        @Override int max(long nanosUntilReset) {
            int decisecondsUntilReset = ((int) nanosUntilReset / NANOS_PER_DECISECOND);
            int index = decisecondsUntilReset == 0 ? 0 : 10 - decisecondsUntilReset;
            return max[index];
        }
    }
}
