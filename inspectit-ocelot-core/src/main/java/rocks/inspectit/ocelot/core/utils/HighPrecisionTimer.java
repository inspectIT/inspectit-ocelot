package rocks.inspectit.ocelot.core.utils;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/**
 * A timer for executing a task with a fixed delay with high precision.
 * This class is designed with the following proeprties:
 * - high precision of the timer (microsecond range)
 * - very low overhead start() method to spin up the timer if called frequently
 * - if the times idles for a long time, it is shutdown automatically (e.g. this timer costs no resources if not used)
 * <p>
 * The timer executes a custom function returning a boolean.
 * The return value hereby represents if any action was done.
 * If no action is performed for a configurable amount of time, the timer automatically shuts down.
 */
@Slf4j
public class HighPrecisionTimer {

    /**
     * The number of nanoseconds of inacitivity after which the timer shuts down.
     */
    private volatile long maximumInactivityNanos;

    /**
     * The period of the timer
     */
    private volatile long periodNanos;

    /**
     * A counter of how many timer executions have passed with no actions being performed ({@link #function} returned false)
     */
    private AtomicLong inactivityCounter;

    /**
     * As soon as {@link #inactivityLimit} reaches this value, the timer shuts itself down
     */
    private volatile long inactivityLimit;

    /**
     * The timer action to execute.
     */
    private BooleanSupplier function;

    /**
     * The timer runnable.
     * Volatile because {@link #start()} could be called from different threads concurrently.
     */
    private volatile TimerRunner timer;

    /**
     * The name of the timer, used for the timer thread.
     */
    private String name;

    /**
     * True if {@link #destroy()} has been called.
     */
    private boolean isDestroyed = false;

    public HighPrecisionTimer(String name, Duration period, Duration maximumInactivity, BooleanSupplier function) {
        inactivityCounter = new AtomicLong();
        this.function = function;
        this.name = name;
        setPeriod(period);
        setMaximumInactivity(maximumInactivity);
    }

    /**
     * Alters the timers period. Can be called while the timer is active.
     * Won't wake-up the timer if it is sleeping for a longer period.
     *
     * @param duration the new period duration
     */
    public synchronized void setPeriod(Duration duration) {
        long newNanos = duration.toNanos();
        if (newNanos != periodNanos) {
            periodNanos = newNanos;
            inactivityLimit = (long) Math.ceil((double) maximumInactivityNanos / periodNanos);
        }
    }

    /**
     * Alters the timers limit of inactivity time.
     * As soon as the timer exceeds the configured limit, it is shut down.
     *
     * @param duration the new inactivity limit
     */
    public synchronized void setMaximumInactivity(Duration duration) {
        long newNanos = duration.toNanos();
        if (newNanos != maximumInactivityNanos) {
            maximumInactivityNanos = newNanos;
            inactivityLimit = (long) Math.ceil((double) maximumInactivityNanos / periodNanos);
            inactivityCounter.set(0L);
        }
    }

    /**
     * Ensures that the timer is running.
     * This method is designed to be very low overhead if the timer is already running.
     */
    public void start() {
        //if we are still far enough away from the inactivity-limit there is no need to enter the synchronized block
        if (timer == null || inactivityCounter.get() > inactivityLimit / 2) {
            startTimerSynchronized();
        } else {
            inactivityCounter.set(0L);
        }
    }

    @VisibleForTesting
    synchronized boolean isStarted() {
        return timer != null;
    }

    @VisibleForTesting
    synchronized void startTimerSynchronized() {
        if (!isDestroyed) {
            inactivityCounter.set(0L);
            if (timer == null) {
                timer = new TimerRunner();
                Thread runner = new Thread(timer);
                runner.setDaemon(true);
                runner.setName(name);
                runner.start();
            }
        }
    }

    /**
     * Destroys this timer. Subsequent {@link #start()} invocations will have no effect.
     */
    public synchronized void destroy() {
        isDestroyed = true;
        if (timer != null && timer.runnerThread != null) {
            timer.runnerThread.interrupt(); //interrupt sleeping
        }
    }

    private synchronized boolean shutdownAfterInactivity() {
        if (inactivityCounter.get() >= inactivityLimit) {
            timer = null;
            return true;
        }
        return false;
    }

    private class TimerRunner implements Runnable {

        private Thread runnerThread;

        @Override
        public void run() {
            log.info("Starting timer {}", name);
            runnerThread = Thread.currentThread();
            while (!isDestroyed && !shutdownAfterInactivity()) {
                long entryTimestamp = System.nanoTime();
                boolean workDone = false;
                try {
                    workDone = function.getAsBoolean();
                } catch (Throwable t) {
                    log.error("Error executing timer function", t);
                }

                if (!workDone) {
                    inactivityCounter.incrementAndGet();
                } else {
                    inactivityCounter.set(0L);
                }

                sleepUntil(entryTimestamp + periodNanos);
            }
            log.debug("Stopping Timer {}", name);
        }

        private void sleepUntil(long nanoTimestamp) {
            long current = System.nanoTime();

            while ((current < nanoTimestamp) && !isDestroyed) {
                LockSupport.parkNanos(nanoTimestamp - current);
                current = System.nanoTime();
            }
        }
    }
}