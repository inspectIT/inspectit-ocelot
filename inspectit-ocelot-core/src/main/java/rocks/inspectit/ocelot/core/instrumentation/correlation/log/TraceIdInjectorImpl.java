package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector;

/**
 * Implementation of {@link TraceIdInjector} for injecting trace ids into log messages.
 * The resulting object will be in the following format if a trace id has been injected:
 * [PREFIX]_trace_id_[SUFFIX]_message_
 */
public class TraceIdInjectorImpl implements TraceIdInjector {

    /**
     * The prefix to use.
     */
    private String prefix;

    /**
     * The suffix to use.
     */
    private String suffix;

    /**
     * Constructor.
     */
    public TraceIdInjectorImpl(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public Object injectTraceId(Object message) {
        String traceId = getTraceId();

        if (traceId != null) {
            return prefix + traceId + suffix + message;
        } else {
            return message;
        }
    }

    /**
     * Returns the current trace id or `null` if non exists.
     */
    private String getTraceId() {
        SpanContext context = Span.current().getSpanContext();
        if (context != null && context.isValid()) {
            return context.getTraceId();
        } else {
            return null;
        }
    }
}
