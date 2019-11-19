package rocks.inspectit.ocelot.bootstrap.correlation.noop;

import rocks.inspectit.ocelot.bootstrap.correlation.TraceIdInjector;

/**
 * No-operation implementation of the {@link TraceIdInjector}.
 */
public class NoopTraceIdInjector implements TraceIdInjector {

    /**
     * Singleton instance.
     */
    public static final TraceIdInjector INSTANCE = new NoopTraceIdInjector();

    private NoopTraceIdInjector() {
    }

    @Override
    public Object injectTraceId(Object message) {
        return message;
    }
}
