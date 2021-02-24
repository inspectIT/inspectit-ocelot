package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

/**
 * Representing the scope of a trace-id injection into a MDC context.
 */
public interface InjectionScope extends AutoCloseable {

    /**
     * A no-operation instance.
     */
    InjectionScope NOOP = () -> {
    };

    @Override
    void close();
}
