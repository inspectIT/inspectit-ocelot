package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector;

public class TraceIdInjectorImpl implements TraceIdInjector {

    private String prefix;

    private String suffix;

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

    private String getTraceId() {
        SpanContext context = Tracing.getTracer().getCurrentSpan().getContext();
        if (context != null && context.isValid()) {
            return context.getTraceId().toLowerBase16();
        } else {
            return null;
        }
    }
}
