package rocks.inspectit.ocelot.bootstrap.correlation;

import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;

public abstract class MdcAccessor implements DoNotInstrumentMarker {

    public interface InjectionScope extends AutoCloseable {

        /**
         * A no-operation instance.
         */
        InjectionScope NOOP = () -> {
        };

        @Override
        void close();
    }

    public abstract Object get(String key);

    public abstract void put(String key, Object value);

    public abstract void remove(String key);

    public abstract boolean isEnabled();

    public InjectionScope inject(String key, String value) {
        put(key, value);

        return () -> {
            remove(key);
        };
    }
}
