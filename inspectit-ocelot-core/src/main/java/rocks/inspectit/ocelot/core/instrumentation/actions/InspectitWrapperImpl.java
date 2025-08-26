package rocks.inspectit.ocelot.core.instrumentation.actions;

import rocks.inspectit.ocelot.bootstrap.exposed.InspectitWrapper;

import java.util.function.BiConsumer;

public class InspectitWrapperImpl implements InspectitWrapper {

    public static final String BEAN_NAME = "wrapper";

    @Override
    public <K, V> BiConsumer<K, V> wrap(BiConsumer<K, V> original) {
        return (k,v) -> {
            System.out.println("Before call");
            original.accept(k, v);
            System.out.println("After call");
        };
    }
}
