package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.trace.Span;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data structure representing the execution of a single method similar to a {@link Span}.
 * However these executions are part of a {@link SampledTrace} and derived via stacktrace sampling.
 */
public class SampledSpan {

    /**
     * The list of child invocations.
     * The order corresponds to the order in which the invocations where executed.
     */
    private List<SampledSpan> children;

    /**
     * The stack trace element defining this method.
     */
    private StackTraceElement methodInfo;

    /**
     * The stack trace element defining this method.
     */
    private StackTraceElement parentFrame;

    /**
     * The start timestamp of this method invoation.
     */
    @Getter
    private long entryTime;

    /**
     * The timestamp when this method invocation finished.
     * This is null if this method invocation has not finished yet.
     */
    @Getter
    private long exitTime = 0;

    public SampledSpan(StackTraceElement method, StackTraceElement parentFrame, long entryTime) {
        methodInfo = method;
        this.entryTime = entryTime;
        this.parentFrame = parentFrame;
    }

    public SampledSpan getLastChild() {
        if (children == null) {
            return null;
        } else {
            return children.get(children.size() - 1);
        }
    }

    public boolean isEnded() {
        return exitTime != 0;
    }

    /**
     * Ends this {@link SampledSpan} with the given end timestamp.
     * Has no effect if the method invocation already ended.
     * In addition, all last children which not have ended yet are also ended.
     *
     * @param timestamp
     */
    public void endWithAllChildren(long timestamp) {
        if (!isEnded()) {
            exitTime = timestamp;
            SampledSpan lastChild = getLastChild();
            if (lastChild != null) {
                lastChild.endWithAllChildren(timestamp);
            }
        }
    }

    public void addChild(SampledSpan child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    public List<SampledSpan> getChildren() {
        return children == null ? Collections.emptyList() : children;
    }

    /**
     * Returns the Class-name without package and the method name concatenated with a dot.
     *
     * @return the name
     */
    public String getSimpleName() {
        String className = methodInfo.getClassName();
        return className.substring(className.lastIndexOf('.') + 1) + "." + methodInfo.getMethodName();
    }

    /**
     * Returns FQN of the class and the method name concatenated with a dot.
     *
     * @return the name
     */
    public String getFullName() {
        String className = methodInfo.getClassName();
        return className + "." + methodInfo.getMethodName();
    }

    /**
     * @return the source location from where this method was called or null if it is not available.
     */
    public String getCallOrigin() {
        if (parentFrame != null) {
            String sourceFile = parentFrame.getFileName();
            int line = parentFrame.getLineNumber();
            if (sourceFile != null) {
                if (line > 0) {
                    return sourceFile + ":" + line;
                } else {
                    return sourceFile;
                }
            }
        }
        return null;
    }

    /**
     * The source file declaring the invoked method, if available.
     *
     * @return the file, if available, otherwise null.
     */
    public String getDeclaringSourceFile() {
        return methodInfo.getFileName();
    }

}
