package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import com.google.common.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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

    private StackTraceElement[] data;

    private int size;

    @VisibleForTesting
    StackTrace(StackTraceElement[] stackTrace) {
        data = stackTrace;
        size = stackTrace.length;
        cleanup();
    }

    public static StackTrace createForCurrentThread() {
        return new StackTrace(Thread.currentThread().getStackTrace());
    }

    /**
     * Returns a stacktrace for each of the specified Threads.
     * When more than one thread is requested, {@link Thread#getAllStackTraces()} will be used.
     * The reason is that this way only one safepoint needs to be reached instead of N (where N is the number of Threads)
     *
     * @param threads
     * @return
     */
    public static Map<Thread, StackTrace> createFor(Collection<Thread> threads) {
        if (threads.size() <= 1) {
            return threads.stream()
                    .collect(Collectors.toMap(
                            thread -> thread,
                            thread -> new StackTrace(thread.getStackTrace())
                    ));
        } else {
            //TODO: Optimize here by using the native Thread.dump(Thread[]) method to avoid building stacktraces for all threads?
            Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
            return threads.stream()
                    .collect(Collectors.toMap(
                            thread -> thread,
                            thread -> new StackTrace(Optional.ofNullable(stackTraces.get(thread)).orElse(new StackTraceElement[0]))
                    ));
        }
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
        return (element.getClassName().equals("java.lang.Thread") && element.getMethodName().equals("getStackTrace"))
                || element.getClassName().startsWith("rocks.inspectit.ocelot.core.")
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
}
