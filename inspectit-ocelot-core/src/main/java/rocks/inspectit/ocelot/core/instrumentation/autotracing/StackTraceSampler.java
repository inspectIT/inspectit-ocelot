package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.common.Clock;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Component for executing stack-trace-sampling (=auto-tracing).
 */
@Component
@Slf4j
public class StackTraceSampler {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ScheduledExecutorService scheduledExecutor;

    /**
     * Global map which stores all threads for which stack-trace sampling is currently active.
     * If a thread is present in this map, it will be sampled and the stack trace will be added to the corresponding trace.
     * This implies that {@link #startSampling()} adds the current thread to this map and {@link #finishSampling()} removes it again.
     */
    private ConcurrentHashMap<Thread, SampledTrace> activeSamplings = new ConcurrentHashMap<>();

    private Future<?> sampleTask;

    /**
     * The clock used for timing the stack-traces.
     * This clock msut be the same as used for OpenCensus {@link Span}s, to make sure that the timings are consistent.
     */
    private Clock clock = Tracing.getClock();

    @PostConstruct
    void init() {
        //TODO: replace with a dynamically configurable & starting timer
        sampleTask = scheduledExecutor.scheduleAtFixedRate(this::doSample, 50, 50, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        sampleTask.cancel(false);
    }

    /**
     * Enables stack-trace-sampling for the given scope.
     * The input scope is expected to be a scope which places a trace onto the context (e.g. {@link io.opencensus.trace.Tracer#withSpan(Span)}).
     * The scope is wrapped with start / end sampling instructions and returned.
     * The span which gets active in the given scope is considered to be the root of trace derived via stack-trace-sampling
     *
     * @param spanScope the scope to perform stack trace sampling for
     * @return the wrapped scope or the unchanged spanScope if stack trace sampling could not be enabled
     */
    public AutoCloseable scoped(AutoCloseable spanScope) {
        boolean started = startSampling();
        if (started) {
            return () -> {
                finishSampling();
                spanScope.close();
            };
        } else {
            return spanScope;
        }
    }

    /**
     * Starts stack-trace sampling for the current thread.
     * Sampling will only be activated, if there is an OpenCensus span on the context which also is sampled (in terms of Span-Sampling).
     * Has no effect if stack trace sampling is already enabled for this thread.
     *
     * @return true, if stack trace sampling was enabled, false otherwise
     */
    private boolean startSampling() {
        Thread self = Thread.currentThread();
        if (!activeSamplings.containsKey(self)) {
            Span root = Tracing.getTracer().getCurrentSpan();
            if (root.getContext() == null || !root.getContext().isValid() || !root.getContext().getTraceOptions().isSampled()) {
                return false;
            }
            StackTrace startStackTrace = StackTrace.createForCurrentThread();
            SampledTrace trace = new SampledTrace(root, startStackTrace, clock.nowNanos());
            activeSamplings.put(self, trace);
            return true;
        }
        return false;
    }

    /**
     * Ends the stack-trace sampling for the current thread and exports all resulting spans.
     */
    private void finishSampling() {
        Thread self = Thread.currentThread();
        SampledTrace trace = activeSamplings.remove(self);
        if (trace != null) {
            //TODO: offload the trace finishing and exporting to a different Thread
            trace.end();
            trace.getRoot().getChildren().forEach(
                    child -> convertAndExport(trace.getRootSpan(), child)
            );
        }
    }

    /**
     * Converts a {@link SampledSpan} to a span and exports it (by calling {@link Span#end()}).
     *
     * @param parent The parent span to use for this {@link SampledSpan}
     * @param invoc  the invocation to export as span
     */
    private void convertAndExport(Span parent, SampledSpan invoc) {
        Span span = CustomSpanBuilder.builder("*" + invoc.getSimpleName(), parent)
                .customTiming(invoc.getEntryTime(), invoc.getExitTime(), null)
                .startSpan();
        span.putAttribute("sampled", AttributeValue.booleanAttributeValue(true));
        span.putAttribute("fqn", AttributeValue.stringAttributeValue(invoc.getFullName()));
        String source = invoc.getDeclaringSourceFile();
        if (source != null) {
            span.putAttribute("source", AttributeValue.stringAttributeValue(source));
        }
        String callOrigin = invoc.getCallOrigin();
        if (callOrigin != null) {
            span.putAttribute("calledFrom", AttributeValue.stringAttributeValue(callOrigin));
        }
        span.end();

        invoc.getChildren().forEach(grandChild -> convertAndExport(span, grandChild));
    }

    /**
     * Method invoked by a timer to sample all threads for which stack trace sampling is activated.
     */
    private void doSample() {
        //copy the map to avoid concurrent modifications due to startSampling() and finishSampling()
        Map<Thread, SampledTrace> samplingsCopy = new HashMap<>(activeSamplings);

        Map<Thread, StackTrace> stackTraces = StackTrace.createFor(samplingsCopy.keySet());
        long timestamp = clock.nowNanos();

        for (Thread thread : samplingsCopy.keySet()) {
            SampledTrace trace = samplingsCopy.get(thread);
            StackTrace stackTrace = stackTraces.get(thread);
            if (activeSamplings.get(thread) == trace) { //recheck for concurrent finishSampling() calls
                trace.add(stackTrace, timestamp); //has no effect if the trace was finished concurrently
            }
        }
    }

}
