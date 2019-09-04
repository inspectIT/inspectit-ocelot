package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.Log4J2MDCAdapter;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.MDCAdapter;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.Slf4jMDCAdapter;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import java.util.*;
import java.util.function.Function;

/**
 * Provides access to all MDCs on the classpath through a single interface.
 * MDCs provide loggers with context information similar to environment variables.
 */
@Component
@Slf4j
public class MDCAccess implements IClassDiscoveryListener {

    /**
     * Non-throwing closeable.
     */
    public interface Undo extends AutoCloseable {
        @Override
        void close();
    }

    private static final Map<String, Function<Class<?>, ? extends MDCAdapter>> MDC_ADAPTER_BUILDERS = new HashMap<>();

    @VisibleForTesting
    WeakHashMap<Class<?>, MDCAdapter> activeAdapters = new WeakHashMap<>();

    static {
        MDC_ADAPTER_BUILDERS.put(Slf4jMDCAdapter.MDC_CLASS, Slf4jMDCAdapter::get);
        MDC_ADAPTER_BUILDERS.put(Log4J2MDCAdapter.THREAD_CONTEXT_CLASS, Log4J2MDCAdapter::get);
    }

    /**
     * Places the given value under the given key in ALL MDCs of all loaded logging libraries.
     * This change can be undone by invoking the returned Undo object.
     *
     * @param key   the key under which the given value shall be put into all MDCs
     * @param value the value to insert
     * @return A function for undoing the change in all MDCs (Restoring any previously set value).
     */
    public Undo put(String key, String value) {
        List<MDCAdapter.Undo> undos = new ArrayList<>();
        for (MDCAdapter adapter : activeAdapters.values()) {
            undos.add(adapter.set(key, value));
        }
        return () -> {
            //iterate in reverse order in case of inter-dependencies
            for (int i = undos.size() - 1; i >= 0; i--) {
                undos.get(i).undoChange();
            }
        };
    }

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        newClasses.stream()
                .filter(clazz -> MDC_ADAPTER_BUILDERS.containsKey(clazz.getName()))
                .filter(clazz -> clazz.getClassLoader() != MDCAccess.class.getClassLoader())
                .filter(clazz -> !(clazz.getClassLoader() instanceof DoNotInstrumentMarker))
                .forEach(clazz -> {
                    try {
                        log.debug("Found MDC implementation for log correlation: {}", clazz.getName());
                        MDCAdapter adapter = MDC_ADAPTER_BUILDERS.get(clazz.getName()).apply(clazz);
                        activeAdapters.put(clazz, adapter);
                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }
}
