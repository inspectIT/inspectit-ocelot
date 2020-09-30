package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.implcore.internal.TimestampConverter;
import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.MethodEntryEvent;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.MethodExitEvent;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.StackTraceSampledEvent;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.TraceEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This is the data structure which holds all recorded information to reconstruct a trace based on both instrumented methods
 * as well as collected stack trace samples.
 * Namely, this information is simply a list of events.
 * <p>
 * The actual algorithm for this reconstruction is defined by the {@link InvocationResolver}.
 */
public class SampledTrace {

    /**
     * Function to mark a method invocation started via {@link #newSpanStarted(PlaceholderSpan, String, String)}
     * or {@link #spanContinued(Span, long, String, String)} as finished.
     */
    public interface MethodExitNotifier {

        void methodFinished(long timestamp);
    }

    /**
     * Flag, whether this trace is finished.
     * If it is finished, no more events will be recorded (they will be ignored).
     */
    private volatile boolean isFinished;

    /**
     * A flag indicating whether the sample recording is paused.
     * This flag is not directly used in this class, it should instead be read and respected
     * by the entity which also calls {@link #addStackTrace(StackTrace, long).}
     */
    private volatile boolean isPaused;

    /**
     * In order to reconstruct the stack trace, we need to know how many frames on the stack trace should be ignored.
     * For this purpose we need a stack trace of the method which started the stack-trace sampling.
     * We capture the stack-trace by creating a new Throwable instance (the constructor captures the stack trace).
     * While capturing the stack-trace is expensive, it is even more expensive to load it from the JVM-native structure into java
     * via {@link Throwable#getStackTrace()}. For this reason we "Delay" the loading until it is actually needed.
     */
    private Supplier<StackTrace> rootStackTraceProvider;

    /**
     * The span which acts as a root for all sampled methods.
     */
    private Span rootSpan;

    /**
     * The list storing the sequence of events.
     * E.g. whenever a stack-trace sample is recorded or an instrumented method is entered or exited,
     * a corresponding event is appended to this list.
     * <p>
     * This means that the order of the events in this List corresponds to the order in time in which the events occurred!
     */
    private ArrayList<TraceEvent> events;

    public SampledTrace(Span rootSpan, Supplier<StackTrace> rootStackTraceProvider) {
        this.rootStackTraceProvider = rootStackTraceProvider;
        this.rootSpan = rootSpan;
        events = new ArrayList<>();
        isFinished = false;
        isPaused = false;
    }

    /**
     * Marks this trace as finished, meaning that no future data will be recorded.
     */
    public synchronized void finish() {
        isFinished = true;
    }

    /**
     * Checks if the sampling is currently configured to be paused.
     *
     * @return true, if it is paused
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Changes the pause-state.
     *
     * @param paused true, if sampling should be configured to be paused
     */
    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    /**
     * Called when a new stack trace has been recorded.
     *
     * @param stackTrace the new stack trace
     * @param timestamp  the (approximate) timestamp when the stack trace was recorded
     */
    public synchronized void addStackTrace(StackTrace stackTrace, long timestamp) {
        if (!isFinished) {
            events.add(new StackTraceSampledEvent(stackTrace, timestamp));
        }
    }

    /**
     * Should be called when an instrumented method is called within this trace.
     * This ensures that the method is correctly placed between the sampled method calls.
     *
     * @param span       the placeholder for the newly started method
     * @param className  the class declaring the method which was invoked (used to locate it within stack traces)
     * @param methodName the name of the method which has been invoked (used to locate it within stack traces)
     *
     * @return a callback which must be invoked as soon as the method finishes.
     */
    public synchronized MethodExitNotifier newSpanStarted(PlaceholderSpan span, String className, String methodName) {
        if (!isFinished) {
            MethodEntryEvent entryEvent = new MethodEntryEvent(span, null, span.getStartTime(), className, methodName, null);
            events.add(entryEvent);
            return (exitTime) -> addExit(entryEvent, exitTime);
        }
        return (time) -> {
        }; //Return NOOP if already finished
    }

    /**
     * Should be called when an instrumented method is called within this trace, which however does continue an exisitign span instead of creating a new one.
     * This ensures that the method is correctly placed between the sampled method calls.
     *
     * @param span       the span which was continued
     * @param startTime  the entry timestamp of the method call
     * @param className  the class declaring the method which was invoked (used to locate it within stack traces)
     * @param methodName the name of the method which has been invoked (used to locate it within stack traces)
     *
     * @return a callback which must be invoked as soon as the method finishes.
     */
    public synchronized MethodExitNotifier spanContinued(Span span, long startTime, String className, String methodName) {
        if (!isFinished) {
            MethodEntryEvent entryEvent = new MethodEntryEvent(null, span, startTime, className, methodName, null);
            events.add(entryEvent);
            return (exitTime) -> addExit(entryEvent, exitTime);
        }
        return (time) -> {
        }; //Return NOOP if already finished
    }

    private synchronized void addExit(MethodEntryEvent entryEvent, long timestamp) {
        if (!isFinished) {
            events.add(new MethodExitEvent(entryEvent, timestamp));
        }
    }

    /**
     * Reconstructs the trace from the series of stack trace samples and method entry / exit events.
     * The reconstructed trace is exported via open-census.
     */
    public void export() {
        Iterable<Invocation> invocations = generateInvocations();
        for (Invocation invoc : invocations) {
            exportWithChildren(invoc, rootSpan);
        }
    }

    /**
     * Converts the list of events to a tree of {@link Invocation}s.
     *
     * @return
     */
    @VisibleForTesting
    Iterable<Invocation> generateInvocations() {
        StackTrace rootTrace = rootStackTraceProvider.get();
        int rootDepth = rootTrace.size() - 1;
        Iterable<Invocation> invocations = InvocationResolver.convertEventsToInvocations(events, rootDepth);
        return invocations;
    }

    private void exportWithChildren(Invocation invoc, Span parentSpan) {
        Span span;
        if (invoc.getSampledMethod() == null) {
            if (invoc.getPlaceholderSpan() != null) {
                span = invoc.getPlaceholderSpan();
                addHiddenParentsAttribute(span, invoc);
                invoc.getPlaceholderSpan().exportWithParent(parentSpan, getTimestampConverter());
            } else {
                span = invoc.getContinuedSpan();
            }
        } else {
            if (!invoc.isHidden()) {
                span = CustomSpanBuilder.builder("*" + getSimpleName(invoc.getSampledMethod()), parentSpan)
                        .customTiming(invoc.getStart().getTimestamp(), invoc.getEnd()
                                .getTimestamp(), getTimestampConverter())
                        .startSpan();
                span.putAttribute("java.sampled", AttributeValue.booleanAttributeValue(true));
                span.putAttribute("java.fqn", AttributeValue.stringAttributeValue(getFullName(invoc.getSampledMethod())));
                addHiddenParentsAttribute(span, invoc);
                span.end();
            } else {
                span = parentSpan;
            }
        }
        for (Invocation child : invoc.getChildren()) {
            exportWithChildren(child, span);
        }
    }

    private void addHiddenParentsAttribute(Span span, Invocation invoc) {
        List<Invocation> hiddenParents = new ArrayList<>();
        Invocation parent = invoc.getParent();
        while (parent != null && parent.isHidden()) {
            hiddenParents.add(parent);
            parent = parent.getParent();
        }
        if (hiddenParents.size() > 0) {
            Collections.reverse(hiddenParents);
            String parents = hiddenParents.stream()
                    .map(inv -> inv.getSampledMethod().toString())
                    .collect(Collectors.joining("\n"));
            span.putAttribute("java.hidden_parents", AttributeValue.stringAttributeValue(parents));
        }
    }

    private TimestampConverter getTimestampConverter() {
        return CustomSpanBuilder.getTimestampConverter((RecordEventsSpanImpl) rootSpan);
    }

    private String getSimpleName(StackTraceElement element) {
        String className = element.getClassName();
        return className.substring(className.lastIndexOf('.') + 1) + "." + element.getMethodName();
    }

    private String getFullName(StackTraceElement element) {
        String className = element.getClassName();
        return className + "." + element.getMethodName();
    }

}
