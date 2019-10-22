package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import io.opencensus.trace.SpanContext;
import io.opencensus.trace.TraceId;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.correlation.LogTraceCorrelator;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Implementation for the {@link LogTraceCorrelator}.
 * Is not a {@link org.springframework.stereotype.Component} because it implements a bootstrap interface.
 * This class does not place itself in {@link rocks.inspectit.ocelot.bootstrap.Instances} by itself,
 * this is done by the {@link LogTraceCorrelationActivator}.
 */
@AllArgsConstructor
@Slf4j
public class LogTraceCorrelatorImpl implements LogTraceCorrelator {

    public static final String BEAN_NAME = "logTraceCorrelator";

    private MDCAccess mdc;

    @Setter
    private String traceIdKey;

    private final Tracer tracer = Tracing.getTracer();

    @Override
    public AutoCloseable startCorrelatedSpanScope(Supplier<? extends AutoCloseable> spanScopeStarter) {
        TraceId oldId = tracer.getCurrentSpan().getContext().getTraceId();
        AutoCloseable spanScope = spanScopeStarter.get();
        SpanContext newContext = tracer.getCurrentSpan().getContext();
        TraceId newId = newContext.getTraceId();
        if (oldId.equals(newId) || !newContext.isValid() || !newContext.getTraceOptions().isSampled()) {
            return spanScope;
        } else {
            AutoCloseable undoMdcChanges = applyCorrelationForTraceContext(newContext);
            return () -> {
                undoMdcChanges.close();
                spanScope.close();
            };
        }
    }

    private MDCAccess.Undo applyCorrelationForTraceContext(SpanContext context) {
        if (context.getTraceId().isValid() && context.getTraceOptions().isSampled()) {
            log.trace("Adding trace correlation information to MDC");
            return mdc.put(traceIdKey, context.getTraceId().toLowerBase16());
        } else {
            return mdc.put(traceIdKey, null);
        }
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return () -> {
            try (MDCAccess.Undo scope = applyCorrelationToMDC()) {
                runnable.run();
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            try (MDCAccess.Undo scope = applyCorrelationToMDC()) {
                return callable.call();
            }
        };
    }

    @Override
    public MDCAccess.Undo applyCorrelationToMDC() {
        return applyCorrelationForTraceContext(tracer.getCurrentSpan().getContext());
    }
}
