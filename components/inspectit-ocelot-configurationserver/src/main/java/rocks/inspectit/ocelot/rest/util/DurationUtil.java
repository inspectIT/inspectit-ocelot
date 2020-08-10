package rocks.inspectit.ocelot.rest.util;

import java.time.Duration;

public class DurationUtil {

    private static final long DAY_NANOS = Duration.ofDays(1).toNanos();

    private static final long HOUR_NANOS = Duration.ofHours(1).toNanos();

    private static final long MINUTE_NANOS = Duration.ofMinutes(1).toNanos();

    private static final long SECOND_NANOS = Duration.ofSeconds(1).toNanos();

    private static final long MILLISECOND_NANOS = Duration.ofMillis(1).toNanos();

    /**
     * Prints the given duration in a {@link org.springframework.boot.convert.DurationStyle#SIMPLE} compatible format.
     * Hereby, the biggest time unit is chosen which does not require a fraction.
     *
     * @param dur the duration to print
     *
     * @return the printed duration
     */
    public static String prettyPrintDuration(Duration dur) {
        long nanos = dur.toNanos();
        if (nanos == 0) {
            return "0s";
        }
        if (nanos % DAY_NANOS == 0) {
            return (nanos / DAY_NANOS) + "d";
        }
        if (nanos % HOUR_NANOS == 0) {
            return (nanos / HOUR_NANOS) + "h";
        }
        if (nanos % MINUTE_NANOS == 0) {
            return (nanos / MINUTE_NANOS) + "m";
        }
        if (nanos % SECOND_NANOS == 0) {
            return (nanos / SECOND_NANOS) + "s";
        }
        if (nanos % MILLISECOND_NANOS == 0) {
            return (nanos / MILLISECOND_NANOS) + "ms";
        }
        return nanos + "ns";
    }
}
