package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import com.google.common.annotations.VisibleForTesting;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A wrapper for an Array of {@link StackTraceElement}s as returned by {@link Thread#getStackTrace}.
 * This wrapper performs a cleanup removing the following stack frames:
 * - All top stackframes containining inspectit-code or Thread.getStackTrace
 * - All stack frames which are in Lambda-code
 * <p>
 * Lambda-stackframes are removed due to an inconsistency between calling {@link Thread#getStackTrace} for the own vs another thread.
 * When calling it from the own Thread, Lambda expressions are often omitted by the JVM.
 * For this reason we remove them in general to avoid inconsistencies.
 */
public class StackTrace {

    public static final int MAX_DEPTH = 8096;

    private StackTraceElement[] data;

    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    private int size;

    @VisibleForTesting
    StackTrace(StackTraceElement[] stackTrace) {
        data = stackTrace;
        size = stackTrace.length;
        cleanup();
    }

    private StackTrace(StackTraceElement[] data, int size) {
        this.data = data;
        this.size = size;
    }

    /**
     * Creates a stack trace based on the stack trace of a given throwable.
     *
     * @param stackTraceHolder the throwable containing the stack trace.
     *
     * @return a stack trace constructed from the given throwable.
     */
    public static StackTrace createFromThrowable(Throwable stackTraceHolder) {
        return new StackTrace(stackTraceHolder.getStackTrace());
    }

    /**
     * Returns a stacktrace for each of the specified Threads.
     * When more than one thread is requested, {@link Thread#getAllStackTraces()} will be used.
     * The reason is that this way only one safepoint needs to be reached instead of N (where N is the number of Threads)
     *
     * @param threads
     *
     * @return
     */
    public static Map<Thread, StackTrace> createFor(Collection<Thread> threads) {
        long[] ids = threads.stream().mapToLong(Thread::getId).toArray();
        Map<Long, Thread> idsToThreads = threads.stream()
                .collect(Collectors.toMap(Thread::getId, thread -> thread));

        ThreadInfo[] threadInfos = THREAD_BEAN.getThreadInfo(ids, MAX_DEPTH);

        Map<Thread, StackTrace> result = new HashMap<>();

        for (int i = 0; i < threadInfos.length; i++) {
            Thread thread = idsToThreads.get(ids[i]);
            StackTraceElement[] stackTrace = threadInfos[i].getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                result.put(thread, new StackTrace(stackTrace));
            }
        }
        return result;
    }

    private void cleanup() {
        while (isHiddenTop(getStackTop())) {
            removeStackTop();
        }
        int deletedCount = 0;
        for (int i = data.length - 1; i >= data.length - size; i--) {
            if (isLambda(data[i])) {
                deletedCount++;
            } else {
                data[i + deletedCount] = data[i];
            }
        }
        size -= deletedCount;
    }

    private boolean isHiddenTop(StackTraceElement element) {
        return element.getClassName().startsWith("rocks.inspectit.ocelot.core.")
                || element.getClassName().startsWith("rocks.inspectit.ocelot.bootstrap.")
                || isLambda(element);
    }

    private boolean isLambda(StackTraceElement element) {
        String className = element.getClassName();
        return className.contains("$Lambda$");
    }

    public StackTraceElement get(int indexFromRoot) {
        return data[data.length - 1 - indexFromRoot];
    }

    public StackTraceElement getStackTop() {
        return get(size - 1);
    }

    public void removeStackTop() {
        size--;
    }

    public int size() {
        return size;
    }

    /**
     * Creates a stack trace from this trace by returning the elements from the root up to the given depth.
     *
     * @param size the depth of the sub-stack trace t oreturn.
     *
     * @return the new stack trace
     */
    public StackTrace createSubTrace(int size) {
        return new StackTrace(data, size);
    }
}
