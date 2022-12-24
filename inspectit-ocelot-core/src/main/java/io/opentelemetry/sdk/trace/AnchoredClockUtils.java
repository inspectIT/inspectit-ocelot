package io.opentelemetry.sdk.trace;

import io.opentelemetry.sdk.common.Clock;
import rocks.inspectit.ocelot.core.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class for the {@link AnchoredClock}, which is package-private.
 */
public class AnchoredClockUtils {

    /**
     * The class of {@link AnchoredClock}
     */
    private final static Class<?> ANCHOREDCLOCK_CLASS;

    /**
     * The {@link AnchoredClock#create(Clock)} method of {@link AnchoredClock}
     */
    private final static Method ANCHOREDCLOCK_CREATE;

    private final static Constructor<?> ANCHOREDCLOCK_CONSTRUCTOR;

    /**
     * The {@link AnchoredClock#startTime()} method of {@link AnchoredClock}
     */
    private final static Method ANCHOREDCLOCK_STARTTIME;

    /**
     * The {@link AnchoredClock#nanoTime} member of {@link AnchoredClock}
     */
    private final static Field ANCHOREDCLOCK_NANOTIME;

    /**
     * The {@link AnchoredClock#clock clock} member of {@link AnchoredClock}
     */
    private final static Field ANCHOREDCLOCK_CLOCK;

    static {
        try {

            ANCHOREDCLOCK_CLASS = Class.forName("io.opentelemetry.sdk.trace.AnchoredClock");
            ANCHOREDCLOCK_CREATE = ANCHOREDCLOCK_CLASS.getDeclaredMethod("create", Clock.class);
            ANCHOREDCLOCK_CREATE.setAccessible(true);

            ANCHOREDCLOCK_CONSTRUCTOR = ANCHOREDCLOCK_CLASS.getDeclaredConstructor(Clock.class, long.class, long.class);
            ANCHOREDCLOCK_CONSTRUCTOR.setAccessible(true);

            ANCHOREDCLOCK_STARTTIME = ANCHOREDCLOCK_CLASS.getDeclaredMethod("startTime");
            ANCHOREDCLOCK_STARTTIME.setAccessible(true);

            ANCHOREDCLOCK_CLOCK = ReflectionUtils.getFieldAndMakeAccessible(ANCHOREDCLOCK_CLASS, "clock");

            ANCHOREDCLOCK_NANOTIME = ReflectionUtils.getFieldAndMakeAccessible(ANCHOREDCLOCK_CLASS, "nanoTime");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link AnchoredClock#startTime()} for the given {@link AnchoredClock}
     *
     * @param anchoredClock
     *
     * @return
     */
    public static long getStartTime(Object anchoredClock) {
        try {
            return (long) ANCHOREDCLOCK_STARTTIME.invoke(anchoredClock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns the {@link AnchoredClock#nanoTime} for the given {@link AnchoredClock}
     *
     * @param anchoredClock
     *
     * @return
     */
    public static long getNanoTime(Object anchoredClock) {
        try {
            return (long) ANCHOREDCLOCK_NANOTIME.get(anchoredClock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not get nanoTime of AnchoredClock", e);
        }

    }

    /**
     * Returns the {@link AnchoredClock#clock clock} for the given {@link AnchoredClock}
     *
     * @param anchoredClock
     *
     * @return
     */
    public static Clock getClock(Object anchoredClock) {
        try {
            return (Clock) ANCHOREDCLOCK_CLOCK.get(anchoredClock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the {@link AnchoredClock#clock} for the given {@link AnchoredClock}
     *
     * @param anchoredClock
     * @param clock
     */
    public static void setClock(Object anchoredClock, Clock clock) {
        try {
            ANCHOREDCLOCK_CLOCK.set(anchoredClock, clock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an {@link AnchoredClock} for the given {@link Clock}
     *
     * @param clock the {@link Clock} to be used to read the current epoch time and nanoTime.
     *
     * @return a {@code MonotonicClock}
     */
    public static Object create(Clock clock) {
        try {
            return ANCHOREDCLOCK_CREATE.invoke(null, clock);
        } catch (Exception e) {
            throw new RuntimeException("Could not create AnchoredClock (" + ANCHOREDCLOCK_CREATE.getDeclaringClass() + "." + ANCHOREDCLOCK_CREATE.getName() + ")", e);
        }
    }

    /**
     * Creates an {@link AnchoredClock} with {@link AnchoredClock#AnchoredClock(Clock, long, long)}
     *
     * @param clock
     * @param epochNanos
     * @param nanoTime
     *
     * @return
     */
    public static Object create(Clock clock, long epochNanos, long nanoTime) {
        try {
            return ANCHOREDCLOCK_CONSTRUCTOR.newInstance(clock, epochNanos, nanoTime);
        } catch (Exception e) {
            throw new RuntimeException("Could not create AnchoredClock", e);
        }
    }

    /**
     * Returns {@code object instanceof AnchoredClock.class}
     *
     * @param clock
     *
     * @return
     */
    public static boolean isInstance(Object clock) {
        return ANCHOREDCLOCK_CLASS.isInstance(clock);
    }

}
