package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

public interface InjectionScope extends AutoCloseable {

    /**
     * A no-operation instance.
     */
    InjectionScope NOOP = () -> {
    };

    @Override
    void close();
}
