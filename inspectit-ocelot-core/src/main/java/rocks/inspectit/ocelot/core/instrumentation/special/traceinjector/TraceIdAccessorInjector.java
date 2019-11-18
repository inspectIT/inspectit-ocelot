package rocks.inspectit.ocelot.core.instrumentation.special.traceinjector;

import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;

import javax.annotation.PostConstruct;

@Component
public class TraceIdAccessorInjector {

    @PostConstruct
    public void injectAccessor() {
        Instances.traceIdAccessor = () -> {
            SpanContext context = Tracing.getTracer().getCurrentSpan().getContext();
            if (context.isValid()) {
                return context.getTraceId().toLowerBase16();
            } else {
                return null;
            }
        };
    }
}
