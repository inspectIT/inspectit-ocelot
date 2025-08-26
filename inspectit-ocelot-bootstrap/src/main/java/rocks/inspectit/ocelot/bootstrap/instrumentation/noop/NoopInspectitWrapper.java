package rocks.inspectit.ocelot.bootstrap.instrumentation.noop;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitWrapper;

import java.util.function.BiConsumer;

public class NoopInspectitWrapper implements InspectitWrapper {

    public static final NoopInspectitWrapper INSTANCE = new NoopInspectitWrapper();

    @Override
    public <K, V> BiConsumer<K, V> wrap(BiConsumer<K, V> original) {
        return original;
    }
}
