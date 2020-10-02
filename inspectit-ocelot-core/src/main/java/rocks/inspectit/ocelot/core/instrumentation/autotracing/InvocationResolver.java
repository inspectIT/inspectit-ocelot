package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.MethodEntryEvent;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.MethodExitEvent;
import rocks.inspectit.ocelot.core.instrumentation.autotracing.events.TraceEvent;

import java.util.*;

/**
 * This class contains the algorithm for converting a series of {@link TraceEvent}s
 * into a tree of {@link Invocation}s.
 */
public class InvocationResolver {

    /**
     * Reconstructs a tree of {@link Invocation}s from the given series of events.
     *
     * @param events              the list of events (stack trace samples, method entry and exists) in the order they were recorded in time
     * @param rootStackTraceDepth the depth at which the root-method of this trace is located within the stack-traces.
     *
     * @return an ordered list of Invocations representing all method calls at the level rootStackTraceDepth+1 (=the children of the root method)
     */
    public static Iterable<Invocation> convertEventsToInvocations(List<TraceEvent> events, int rootStackTraceDepth) {
        Map<MethodEntryEvent, Integer> durations = getMethodDurations(events);
        List<TraceEvent> enhancedEvents = new ArrayList<>();
        assignStackTracesToMethodEntries(events, durations, rootStackTraceDepth + 1, enhancedEvents);
        if (events.size() != enhancedEvents.size()) {
            throw new IllegalStateException("Something went wrong!");
        }

        Invocation rootInvocation = new Invocation(null, null, null); //dummy object, just used as a list
        resolveChildInvocations(enhancedEvents, rootStackTraceDepth + 1, rootInvocation);
        return rootInvocation.getChildren();
    }

    /**
     * In order to better locate the instrumented methods between the sampled ones,
     * we need to have a stack trace for each method entry event.
     * <p>
     * This function does this for each instrumented method by checking if any stack trace
     * was recorded between the start and the end of the method.
     * By nature, this stack-trace will contain the instrumented method itself.
     * Therefore we search the stack trace for the method and remove all frames below it.
     * This new stack trace can then be used as stack trace for the method.
     *
     * @param events     the input events to analyze
     * @param durations  an index for the method durations (computed via {@link #getMethodDurations(List)}
     * @param startDepth the depth at which we start looking for the methods within the stack traces.
     * @param output     All events from the "events" argument will be appended to this output list, but possibly enhanced with better stack traces.
     */
    private static void assignStackTracesToMethodEntries(List<TraceEvent> events, Map<MethodEntryEvent, Integer> durations, int startDepth, List<TraceEvent> output) {
        int offset = 0;
        while (offset < events.size()) {
            TraceEvent origEvent = events.get(offset);
            if (origEvent instanceof MethodEntryEvent) {
                MethodEntryEvent entryEvent = (MethodEntryEvent) origEvent;
                int duration = durations.get(origEvent);
                MethodExitEvent exitEvent = (MethodExitEvent) events.get(offset + duration);
                //only consider events between start and end of the method
                List<TraceEvent> methodEvents = events.subList(offset, offset + duration + 1);
                Optional<TraceEvent> firstWithStackTrace = methodEvents
                        .stream()
                        .filter(event -> event.getStackTraceElementAt(startDepth) != null) //find an element which has a stack trace
                        .findFirst();
                if (firstWithStackTrace.isPresent()) {
                    StackTrace trace = firstWithStackTrace.get().getStackTrace();
                    int currentDepth = startDepth;
                    while (currentDepth < trace.size() && !isSameMethod(trace.get(currentDepth), entryEvent)) {
                        currentDepth++;
                    }
                    // After the loop, currentDepth is bigger than the trace OR points at the found span
                    // if currentDepth is bigger than the trace, this means that no matching method has been found
                    // This can happen because the timings of stack trace samples are inaccurate, leading to samples "sliding" in
                    // ATM, we just accept this and use the invalid stack trace as parent, leading to potential inaccuracies
                    // In the future, we should remove such "invalid" samples from the list of events to prevent this.
                    StackTrace cutTrace = trace.createSubTrace(currentDepth);
                    MethodEntryEvent newEntry = entryEvent.copyWithNewStackTrace(cutTrace);
                    MethodExitEvent newExit = new MethodExitEvent(newEntry, exitEvent.getTimestamp());

                    output.add(newEntry);
                    assignStackTracesToMethodEntries(methodEvents.subList(1, methodEvents.size() - 1), durations, currentDepth + 1, output);
                    output.add(newExit); //add the new end-event
                } else {
                    //no stack trace occured between method start and end, keep the events as-is
                    output.addAll(methodEvents);
                }
                offset += duration + 1; //point after the exit event!
            } else if (origEvent instanceof MethodExitEvent) {
                throw new IllegalStateException("This case should never happen!");
            } else {
                output.add(origEvent); //keep unchanged
                offset++;
            }
        }
    }

    private static boolean isSameMethod(StackTraceElement stackTraceElement, MethodEntryEvent entryEvent) {
        return entryEvent.getClassName().equals(stackTraceElement.getClassName())
                && entryEvent.getMethodName().equals(stackTraceElement.getMethodName());
    }

    /**
     * Computes the duration of every observed instrumented method call.
     * Duration hereby does NOT mean time! Instead the number of events from start to end is counted.
     * E.g. given an entry event at index i in the event list, its corresponding exit event will be found at the index i+duration.
     *
     * @param events the list of events in the order they appeared.
     *
     * @return the duration map
     */
    private static Map<MethodEntryEvent, Integer> getMethodDurations(List<TraceEvent> events) {
        Map<MethodEntryEvent, Integer> entryIndex = new HashMap<>();
        Map<MethodEntryEvent, Integer> durations = new HashMap<>();
        for (int i = 0; i < events.size(); i++) {
            TraceEvent event = events.get(i);
            if (event instanceof MethodEntryEvent) {
                entryIndex.put((MethodEntryEvent) event, i);
            } else if (event instanceof MethodExitEvent) {
                MethodEntryEvent entryEvent = ((MethodExitEvent) event).getEntryEvent();
                int startIndex = entryIndex.get(entryEvent);
                durations.put(entryEvent, i - startIndex);
            }
        }
        return durations;
    }

    /**
     * Given an Invocation which is known to be at stack-trace depth x,
     * this method will try to find all its child invocations (method calls at depth x+1).
     * <p>
     * In order to do so, this method needs the list of events which have occurred within the duration of this parent Invocation.
     *
     * @param events the list of events which have occurred during the execution of the parent invocation
     * @param depth  the depth at which we scan for the child invocation. This implies that the parent invocation is located at (depth-1).
     * @param parent the parent invocation to which the found child-invocations will be added as children.
     */
    private static void resolveChildInvocations(List<TraceEvent> events, int depth, Invocation parent) {
        //walk the events left-to-right to identify the calls
        int offset = 0;
        while (offset < events.size()) {
            TraceEvent current = events.get(offset);
            if (current.getStackTraceElementAt(depth) != null) {
                int endIndex = findLastEqualEvent(events, offset, depth);
                endIndex = extendEndToIncludeInstrumentations(events, offset, endIndex);
                if (offset != endIndex) { //at least two events have fallen into the range
                    resolveSampledInvocation(events.subList(offset, endIndex + 1), depth, parent);
                }
                offset = endIndex + 1; //continue with all unprocessed events
            } else if (current instanceof MethodEntryEvent) {
                int endIndex = findMethodExitEvent(events, offset);
                resolveInstrumentedInvocation(events.subList(offset, endIndex + 1), depth, parent);
                offset = endIndex + 1; //continue with the next child
            } else {
                offset++;
            }
        }
    }

    /**
     * Walks the list of events in time, searching for the first event which does not have the same method at the given stack trace depth.
     * As soon as such an event is encountered, the index of the last matching event is returned.
     *
     * @param events       the list of events to scan
     * @param compareIndex we search for the last element relative to this index which is equal to events[compareIndex]
     * @param depth        the depth at which operate within the stack traces
     *
     * @return the index of the last matching event (which is potentially equal to compareIndex).
     */
    private static int findLastEqualEvent(List<TraceEvent> events, int compareIndex, int depth) {
        TraceEvent compareTo = events.get(compareIndex);
        StackTraceElement cmpParent = compareTo.getStackTraceElementAt(depth - 1);
        StackTraceElement cmpElement = compareTo.getStackTraceElementAt(depth);
        int lastMatchingIndex = compareIndex;
        for (int i = compareIndex + 1; i < events.size(); i++) {
            TraceEvent other = events.get(i);
            if (other.getStackTrace() != null) { //ignore events without stacktraces
                StackTraceElement parent = other.getStackTraceElementAt(depth - 1);
                StackTraceElement element = other.getStackTraceElementAt(depth);
                if (element == null) { //stack trace is not deep enough
                    return lastMatchingIndex;
                }
                if (!stackTraceElementsEqual(cmpElement, cmpParent, element, parent)) {
                    return lastMatchingIndex;
                } else {
                    lastMatchingIndex = i;
                }
            }
        }
        return lastMatchingIndex; // all are equal
    }

    /**
     * Given a list of events and a range [startIndex,endIndex] within it.
     * This method extends endIndex, in case method calls where started within this range and not finished.
     * This ensures that no method calls are started but not finsihed within the range [startIndex,resultIndex].
     *
     * @param events     the list to scan
     * @param startIndex the start of the range to (potentially) extend (inclusive)
     * @param endIndex   the end of the range to (potentially) extend (inclusive)
     *
     * @return an index x greater or equal to endIndex, so that the within the range [startIndex, x] no method calls are started but not ended
     */
    private static int extendEndToIncludeInstrumentations(List<TraceEvent> events, int startIndex, int endIndex) {
        int entryCount = 0;
        int newEnd = startIndex - 1;
        while (newEnd < endIndex || entryCount != 0) {
            newEnd++;
            if (events.get(newEnd) instanceof MethodEntryEvent) {
                entryCount++;
            } else if (events.get(newEnd) instanceof MethodExitEvent) {
                entryCount--;
            }
        }
        return newEnd;
    }

    private static boolean stackTraceElementsEqual(StackTraceElement first, StackTraceElement parentOfFirst, StackTraceElement second, StackTraceElement parentOfSecond) {
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

    private static int findMethodExitEvent(List<TraceEvent> events, int methodEntryIndex) {
        MethodEntryEvent entry = (MethodEntryEvent) events.get(methodEntryIndex);
        for (int i = methodEntryIndex + 1; i < events.size(); i++) {
            if (events.get(i) instanceof MethodExitEvent) {
                MethodExitEvent exit = (MethodExitEvent) events.get(i);
                if (exit.getEntryEvent() == entry) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Could not find a matching Exit-Event!");
    }

    private static void resolveInstrumentedInvocation(List<TraceEvent> events, int depth, Invocation parent) {
        MethodEntryEvent start = (MethodEntryEvent) events.get(0);
        MethodExitEvent end = (MethodExitEvent) events.get(events.size() - 1);

        Invocation instrumentedMethod = new Invocation(start, end, start.getPlaceholderSpan(), start.getContinuedSpan());
        parent.addChild(instrumentedMethod);

        List<TraceEvent> eventsWithoutStartAndEnd = events.subList(1, events.size() - 1);
        resolveChildInvocations(eventsWithoutStartAndEnd, depth + 1, instrumentedMethod);
    }

    private static void resolveSampledInvocation(List<TraceEvent> events, int depth, Invocation parent) {
        TraceEvent startEvent = events.get(0);
        TraceEvent endEvent = events.get(events.size() - 1);

        StackTraceElement element = startEvent.getStackTrace().get(depth);
        Invocation sampledMethod = new Invocation(startEvent, endEvent, element);
        parent.addChild(sampledMethod);

        resolveChildInvocations(events, depth + 1, sampledMethod);
    }

}
