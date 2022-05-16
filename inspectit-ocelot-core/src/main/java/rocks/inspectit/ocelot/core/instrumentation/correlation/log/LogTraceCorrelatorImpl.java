package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import io.opencensus.trace.SpanContext;
import io.opencensus.trace.TraceId;
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
@Slf4j
@AllArgsConstructor
public class LogTraceCorrelatorImpl implements LogTraceCorrelator {

    public static final String BEAN_NAME = "logTraceCorrelator";

    /**
     * Accessor for the MDCs.
     */
    private MdcAccessManager mdcAccess;

    @Setter
    private String traceIdKey;

    @Override
    public AutoCloseable startCorrelatedSpanScope(Supplier<? extends AutoCloseable> spanScopeStarter) {
        TraceId oldId = Tracing.getTracer().getCurrentSpan().getContext().getTraceId();
        AutoCloseable spanScope = spanScopeStarter.get();
        SpanContext newContext = Tracing.getTracer().getCurrentSpan().getContext();
        TraceId newId = newContext.getTraceId();
        if (oldId.equals(newId) || !newContext.isValid() || !newContext.getTraceOptions().isSampled()) {
            return spanScope;
        } else {
            InjectionScope injectionScope = injectTraceContextInMdc(newContext);
            return () -> {
                injectionScope.close();
                spanScope.close();
            };
        }
    }

    /**
     * Injects the given span context into all configured MDCs.
     *
     * @param context the context to inject
     *
     * @return an {@link InjectionScope} to revert the injection and restore the initial state of the MDC
     */
    private InjectionScope injectTraceContextInMdc(SpanContext context) {
        if (context.getTraceId().isValid() && context.getTraceOptions().isSampled()) {
            log.trace("Adding trace correlation information to MDC.");
            return mdcAccess.injectValue(traceIdKey, context.getTraceId().toLowerBase16());
        } else {
            return mdcAccess.injectValue(traceIdKey, null);
        }
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return () -> {
            try (InjectionScope scope = injectTraceIdIntoMdc()) {
                runnable.run();
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            try (InjectionScope scope = injectTraceIdIntoMdc()) {
                return callable.call();
            }
        };
    }

    @Override
    public InjectionScope injectTraceIdIntoMdc() {
        return injectTraceContextInMdc(Tracing.getTracer().getCurrentSpan().getContext());
    }
}


