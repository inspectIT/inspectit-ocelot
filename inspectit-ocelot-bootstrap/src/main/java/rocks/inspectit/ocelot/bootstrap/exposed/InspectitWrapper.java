package rocks.inspectit.ocelot.bootstrap.exposed;

import java.util.function.BiConsumer;

public interface InspectitWrapper {

    <K, V> BiConsumer<K, V> wrap(BiConsumer<K, V> original);
}
