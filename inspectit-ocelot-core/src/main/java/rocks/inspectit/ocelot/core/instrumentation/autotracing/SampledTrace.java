package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.trace.Span;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * A Trace build by analyzing subsequent {@link StackTrace}s.
 * In general, whenever a method appears in two subsequent StackTrace samples, it will result in a {@link SampledSpan}.
 * <p>
 * This class is thread safe: {@link #add(StackTrace, long)} and {@link #end()} can be called concurrently.
 * It is guaranteed that after {@link #end()} has been called, the trace will not be modified anymore.
 */
@Slf4j
public class SampledTrace {

    /**
     * The root of the resulting sampled trace.
     * Corresponds to the method which started the sampling (an instrumented method with autoTracing=true).
     */
    @Getter
    private SampledSpan root;

    /**
     * The span of {@link #root}, always exists because only instrumented methods can enable autoTracing.
     */
    @Getter
    private Span rootSpan;

    /**
     * Defines the index from the bottom of the stack trace where {@link #root} lies on it.
     * E.g. if the stack trace looks like this:
     * <p>
     * MainClass.main()
     * MainClass.doSomething()
     * MyClass.instrumented()
     * <p>
     * And the method starting the stack-trace sampling is "MyClass.instrumented", the rootDepth is 2
     */
    private int rootDepth;

    /**
     * We create {@link SampledSpan}s based on whether we see the same method invocation in two subsequent stacktraces.
     * Therefore we remember the previous stack trace in this field.
     */
    private StackTrace previousStackTrace;

    /**
     * The timestamp when {@link #previousStackTrace} was collected.
     * Used to mark {@link SampledSpan} when they are not found anymore on the new stack-trace.
     */
    private long previousTimeStamp;

    public SampledTrace(Span rootSpan, StackTrace rootStackTrace, long timestamp) {
        rootDepth = rootStackTrace.size() - 1;
        previousStackTrace = rootStackTrace;
        if (rootDepth > 0) {
            root = new SampledSpan(rootStackTrace.get(rootDepth), null, timestamp);
        } else {
            root = new SampledSpan(rootStackTrace.get(rootDepth), rootStackTrace.get(rootDepth - 1), timestamp);
        }
        previousTimeStamp = timestamp;
        this.rootSpan = rootSpan;
    }

    /**
     * Adds a new stack-trace sample to this trace.
     * Has no effect if {@link #end()} was called before.
     *
     * @param newTrace  The newly observed stack trace
     * @param timestamp the timestamp when this trace was observed
     */
    public synchronized void add(StackTrace newTrace, long timestamp) {
        if (!isFinished()) {
            int minLen = Math.min(newTrace.size(), previousStackTrace.size());

            SampledSpan parent = root;
            for (int i = rootDepth + 1; i < minLen; i++) {
                StackTraceElement prevParent = previousStackTrace.get(i - 1);
                StackTraceElement prev = previousStackTrace.get(i);
                StackTraceElement currParent = newTrace.get(i - 1);
                StackTraceElement curr = newTrace.get(i);
                if (stackTraceElementsEqual(curr, currParent, prev, prevParent)) {
                    SampledSpan child = parent.getLastChild();
                    if (child == null || child.isEnded()) {
                        child = new SampledSpan(curr, currParent, previousTimeStamp);
                        parent.addChild(child);
                    }
                    parent = child;
                } else {
                    break;
                }
            }
            //end all open calls from which we have returned
            SampledSpan child = parent.getLastChild();
            if (child != null && !child.isEnded()) {
                child.endWithAllChildren(previousTimeStamp);
            }
            previousStackTrace = newTrace;
            previousTimeStamp = timestamp;

        }
    }

    private boolean stackTraceElementsEqual(StackTraceElement first, StackTraceElement parentOfFirst, StackTraceElement second, StackTraceElement parentOfSecond) {
        if (!Objects.equals(first.getMethodName(), second.getMethodName())) {
            return false;
        }
        if (!Objects.equals(first.getClassName(), second.getClassName())) {
            return false;
        }
        if (!Objects.equals(parentOfFirst.getFileName(), parentOfSecond.getFileName())) {
            return false;
        }
        if (parentOfFirst.getLineNumber() != parentOfSecond.getLineNumber()) {
            return false;
        }
        return true;
    }

    /**
     * Marks this trace as finished, meaning that the last received stack-trace ends all method calls.
     */
    public synchronized void end() {
        if (!root.isEnded()) {
            root.endWithAllChildren(previousTimeStamp);
        }
    }

    /**
     * @return true, if {@link #end()} was called, false otherwise
     */
    public synchronized boolean isFinished() {
        return root.isEnded();
    }
}
