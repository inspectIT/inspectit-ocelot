package rocks.inspectit.ocelot.bootstrap.correlation.noop;

import rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector;

public class NoopTraceIdInjector implements TraceIdInjector {

    public static final TraceIdInjector INSTANCE = new NoopTraceIdInjector();

    @Override
    public Object injectTraceId(Object message) {
        return message;
    }
}
