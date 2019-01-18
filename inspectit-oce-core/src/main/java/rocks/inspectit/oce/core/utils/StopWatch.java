package rocks.inspectit.oce.core.utils;

import java.util.concurrent.TimeUnit;

/**
 * Simple stop watch for measuring time with high precision.
 */
public class StopWatch {

    private long start = System.nanoTime();

    /**
     * Returns the time elapsed until now since the creation of this {@link StopWatch} in nanoseconds.
     *
     * @return the number of nanoseconds
     */
    public long getElapsedNanos() {
        return System.nanoTime() - start;
    }

    /**
     * Returns the time elapsed until now since the creation of this {@link StopWatch} in milliseconds.
     *
     * @return the number of milliseconds
     */
    public long getElapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(getElapsedNanos());
    }

}
