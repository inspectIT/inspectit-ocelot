package rocks.inspectit.ocelot.bootstrap.correlation.noop;

import rocks.inspectit.ocelot.bootstrap.correlation.TraceIdAccessor;

public class NoopTraceIdAccessor implements TraceIdAccessor {

    public static final TraceIdAccessor INSTANCE = new NoopTraceIdAccessor();

    @Override
    public String getTraceId() {
        return null;
    }
}
