package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.common.Clock;
import io.opencensus.common.Scope;
import io.opencensus.trace.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.tracing.AutoTracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodReflectionInformation;
import rocks.inspectit.ocelot.core.utils.HighPrecisionTimer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Component for executing stack-trace-sampling (=auto-tracing).
 */
@Component
@Slf4j
public class StackTraceSampler {

    /**
     * Stack-trace samples require a post-processing before being exported as traces.
     * This happens asynchronously to the application to minimize the performance impact in a periodicalyl executed task.
     * This value defines the frequency at which the queue is checked.
     */
    public static final int EXPORT_INTERVAL_MILLIS = 200;

    /**
     * The change to apply to the state of the sampler when invoking {@link #createAndEnterSpan(String, SpanContext, Sampler, Span.Kind, MethodReflectionInformation, Mode)}
     * or {@link #continueSpan(Span, MethodReflectionInformation, Mode)}.
     */
    public enum Mode {
        /**
         * Starts stack-trace sampling if it is not already active.
         */
        ENABLE,
        /**
         * Pauses the stack-trace sampling if it was activated by a parent call.
         */
        DISABLE,
        /**
         * Preserves the state of the stack trace sampling for the current thread.
         */
        KEEP
    }

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ScheduledExecutorService executor;

    /**
     * The task periodicalyl executed for ptocessing the {@link #tracesToExport} queue.
     */
    private Future<?> exportTask;

    /**
     * A bounded queue containing all traces which have finished but need to be processed for exporting.
     */
    private ArrayBlockingQueue<SampledTrace> tracesToExport = new ArrayBlockingQueue<>(4096);

    /**
     * Global map which stores all threads for which stack-trace sampling is currently active.
     * If a thread is present in this map, it will be sampled and the stack trace will be added to the corresponding trace.
     */
    private ConcurrentHashMap<Thread, SampledTrace> activeSamplings = new ConcurrentHashMap<>();

    /**
     * The timer used to trigger the capturing of stack-trace samples.
     */
    private HighPrecisionTimer sampleTimer;

    /**
     * The clock used for timing the stack-traces.
     * This clock must be the same as used for OpenCensus {@link Span}s, to make sure that the timings are consistent.
     */
    private Clock clock = Tracing.getClock();

    @PostConstruct
    void init() {
        exportTask = executor.scheduleWithFixedDelay(this::doExportTraces, EXPORT_INTERVAL_MILLIS, EXPORT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        AutoTracingSettings settings = env.getCurrentConfig().getTracing().getAutoTracing();
        sampleTimer = new HighPrecisionTimer("Ocelot stack trace sampler", settings.getFrequency(),
                settings.getShutdownDelay(), this::doSample);
    }

    @EventListener(InspectitConfigChangedEvent.class)
    void updateTimer() {
        AutoTracingSettings settings = env.getCurrentConfig().getTracing().getAutoTracing();
        sampleTimer.setPeriod(settings.getFrequency());
        sampleTimer.setMaximumInactivity(settings.getShutdownDelay());
    }

    @PreDestroy
    void shutdown() {
        exportTask.cancel(false);
        sampleTimer.destroy();
    }

    /**
     * Starts a new span with the given details.
     * <p>
     * If mode is ENABLE and stack trace sampling is not active for the current,
     * this span will generate its children using stack trace sampling.
     * If mode is ENABLE and a parent span already started stack trace sampling,
     * the sampler will attempt to correctly place this span between the stack-trace sample generated ones.
     * <p>
     * If mode is DISABLE and a stack-trace sampling session exists on the current thread, it will be paused.
     * The mode KEEP will simply preserver the current sampling status.
     *
     * @param name         the name of the span to create
     * @param remoteParent the (optional) remote parent of the span, should only be set if this span is a root.
     * @param sampler      the sampler to use (not for stack-trace sampling but for span sampling)
     * @param kind         the (optional) kind of the span
     * @param actualMethod the actual method for which this span is created.
     * @param mode         the mode to use for Stack-Trace sampling.
     *
     * @return The generated span is activated via {@link Tracer#withSpan(Span)}. The resulting context is wrapped with a context which terminates the stack-trace-sampling.
     */
    public AutoCloseable createAndEnterSpan(String name, SpanContext remoteParent, Sampler sampler, Span.Kind kind, MethodReflectionInformation actualMethod, Mode mode) {
        Thread self = Thread.currentThread();
        SampledTrace activeSampling = activeSamplings.get(self);
        if (activeSampling == null) {
            if (mode == Mode.ENABLE) {
                return startSampling(name, remoteParent, sampler, kind);
            } else {
                return Tracing.getTracer().withSpan(createNormalSpan(name, remoteParent, sampler, kind));
            }
        } else {
            AutoCloseable samplingAwareSpanContext = createSamplingAwareSpan(name, remoteParent, kind, actualMethod, activeSampling);
            return createPauseAdjustingContext(mode, activeSampling, samplingAwareSpanContext);
        }
    }

    /**
     * Continues a span but makes it's children stack-trace sampling aware.
     * <p>
     * If mode is ENABLE and stack trace sampling is not active for the current,
     * this span will generate its children using stack trace sampling.
     * If mode is ENABLE and a parent span already started stack trace sampling,
     * the sampler will attempt to correctly place this span between the stack-trace sample generated ones.
     * <p>
     * If mode is DISABLE and a stack-trace sampling session exists on the current thread, it will be paused.
     * The mode KEEP will simply preserver the current sampling status.
     *
     * @param span         the span to continue
     * @param actualMethod the actual method for which this span is made active
     * @param mode         the mode to use for stack trace sampling.
     *
     * @return The span is activated via {@link Tracer#withSpan(Span)}. The resulting context is wrapped with a context which terminates the stack-trace-sampling.
     */
    public AutoCloseable continueSpan(Span span, MethodReflectionInformation actualMethod, Mode mode) {
        Thread self = Thread.currentThread();
        SampledTrace activeSampling = activeSamplings.get(self);
        if (activeSampling == null) {
            if (mode == Mode.ENABLE) {
                return startSampling(span);
            } else {
                return Tracing.getTracer().withSpan(span);
            }
        } else {
            AutoCloseable samplingAwareSpanContext = continueSamplingAwareSpan(span, actualMethod, activeSampling);
            return createPauseAdjustingContext(mode, activeSampling, samplingAwareSpanContext);
        }
    }

    /**
     * Wraps the given spanContext in order to pause the stack-trace sampling if required.
     *
     * @param mode           the mode (used to check whether altering the pause-state is required).
     * @param activeSampling the currently active sampling session
     * @param spanContext    the span-context to wrap
     *
     * @return the wrapped span-context which alters the pause state accordingly.
     */
    private AutoCloseable createPauseAdjustingContext(Mode mode, SampledTrace activeSampling, AutoCloseable spanContext) {
        if (activeSampling.isPaused() && mode == Mode.ENABLE) {
            activeSampling.setPaused(false);
            return () -> {
                spanContext.close();
                activeSampling.setPaused(true);
            };
        } else if (!activeSampling.isPaused() && mode == Mode.DISABLE) {
            activeSampling.setPaused(true);
            return () -> {
                spanContext.close();
                activeSampling.setPaused(false);
            };
        } else {
            return spanContext;
        }
    }

    private AutoCloseable continueSamplingAwareSpan(Span spanToContinue, MethodReflectionInformation actualMethod, SampledTrace activeSampling) {
        SampledTrace.MethodExitNotifier exitCallback = activeSampling
                .spanContinued(spanToContinue, clock.nowNanos(), actualMethod.getDeclaringClass()
                        .getName(), actualMethod.getName());
        Scope ctx = Tracing.getTracer().withSpan(spanToContinue);
        return () -> {
            ctx.close();
            exitCallback.methodFinished(clock.nowNanos());
        };
    }

    private AutoCloseable startSampling(String name, SpanContext remoteParent, Sampler sampler, Span.Kind kind) {
        Span rootSpan = createNormalSpan(name, remoteParent, sampler, kind);
        return startSampling(rootSpan);
    }

    private AutoCloseable startSampling(Span rootSpan) {
        boolean spanExists = rootSpan.getContext().isValid() && rootSpan.getContext().getTraceOptions().isSampled();
        if (!spanExists) {
            return Tracing.getTracer().withSpan(rootSpan);
        } else {
            Throwable stackTrace = new Throwable(); //the constructor collects the current stack-trace
            SampledTrace sampledTrace = new SampledTrace(rootSpan, () -> StackTrace.createFromThrowable(stackTrace));
            Thread selfThread = Thread.currentThread();
            activeSamplings.put(selfThread, sampledTrace);
            sampleTimer.start();
            AutoCloseable spanScope = Tracing.getTracer().withSpan(rootSpan);
            return () -> {
                spanScope.close();
                activeSamplings.remove(selfThread);
                sampledTrace.finish();
                addToExportQueue(sampledTrace);
            };
        }
    }

    private AutoCloseable createSamplingAwareSpan(String name, SpanContext remoteParent, Span.Kind kind, MethodReflectionInformation actualMethod, SampledTrace activeSampling) {
        SpanContext parent = remoteParent;
        if (remoteParent == null) {
            parent = Tracing.getTracer().getCurrentSpan().getContext();
        }
        PlaceholderSpan span = new PlaceholderSpan(parent, name, kind, clock::nowNanos);
        SampledTrace.MethodExitNotifier exitCallback = activeSampling
                .newSpanStarted(span, actualMethod.getDeclaringClass().getName(), actualMethod.getName());
        Scope ctx = Tracing.getTracer().withSpan(span);
        return () -> {
            ctx.close();
            exitCallback.methodFinished(clock.nowNanos());
        };
    }

    private Span createNormalSpan(String name, SpanContext remoteParent, Sampler sampler, Span.Kind kind) {
        SpanBuilder builder;
        if (remoteParent != null) {
            builder = Tracing.getTracer().spanBuilderWithRemoteParent(name, remoteParent);
        } else {
            builder = Tracing.getTracer().spanBuilder(name);
        }
        builder.setSpanKind(kind);
        if (sampler != null) {
            builder.setSampler(sampler);
        }
        Span span = builder.startSpan();
        return span;
    }

    /**
     * Method invoked by a timer to sample all threads for which stack trace sampling is activated.
     * Returns true, if any sampling was performed
     */
    private boolean doSample() {
        //copy the map to avoid concurrent modifications due to the starting and ending of sampling traces
        Map<Thread, SampledTrace> samplingsCopy = new HashMap<>(activeSamplings);

        Set<Thread> threadsToSample = samplingsCopy.keySet().stream()
                .filter(trace -> !samplingsCopy.get(trace).isPaused())
                .collect(Collectors.toSet());

        long timestamp = clock.nowNanos();
        Map<Thread, StackTrace> stackTraces = StackTrace.createFor(threadsToSample);

        boolean anySampled = false;

        for (Thread thread : threadsToSample) {
            SampledTrace trace = samplingsCopy.get(thread);
            StackTrace stackTrace = stackTraces.get(thread);
            if (stackTrace != null) { //recheck for concurrent finishSampling() calls
                anySampled = true;
                trace.addStackTrace(stackTrace, timestamp); //has no effect if the trace was finished concurrently
            }
        }

        return anySampled;
    }

    private void addToExportQueue(SampledTrace sampledTrace) {
        if (!tracesToExport.offer(sampledTrace)) {
            log.warn("Dropping sampled-spans! Please reduce your auto-tracing scope!");
        }
    }

    private void doExportTraces() {
        while (!tracesToExport.isEmpty()) {
            try {
                tracesToExport.poll().export();
            } catch (Exception e) {
                log.error("Error exporting sampled trace", e);
            }
        }
    }

}
