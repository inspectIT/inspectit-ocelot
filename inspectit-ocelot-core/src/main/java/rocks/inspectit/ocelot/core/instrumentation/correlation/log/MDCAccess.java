package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.AgentImpl;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.*;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        /**
         * A no-operation Undo.
         */
        Undo NOOP = () -> {
        };
    }

    @Autowired
    private InspectitEnvironment inspectitEnv;

    /**
     * Maps class names of MDC classes to their corresponding adapter factory methods.
     */
    private final Map<String, Function<Class<?>, ? extends MDCAdapter>> mdcAdapterBuilders = new HashMap<>();

    /**
     * Holds references to all MDC Adapters for all classes found on the classpath.
     */
    @VisibleForTesting
    WeakHashMap<Class<?>, MDCAdapter> availableAdapters = new WeakHashMap<>();

    /**
     * Holds references to all MDC Adapters which are enabled.
     * The adapters are weakly referenced because their livetime is controlled by {@link #availableAdapters}.
     */
    @VisibleForTesting
    Set<MDCAdapter> enabledAdapters = Collections.emptySet();

    /**
     * Registers all implemented log api MDC adapters.
     */
    @PostConstruct
    void registerAdapters() {
        mdcAdapterBuilders.put(Slf4jMDCAdapter.MDC_CLASS, Slf4jMDCAdapter::get);
        mdcAdapterBuilders.put(Log4J2MDCAdapter.THREAD_CONTEXT_CLASS, Log4J2MDCAdapter::get);
        mdcAdapterBuilders.put(Log4J1MDCAdapter.MDC_CLASS, Log4J1MDCAdapter::get);
        mdcAdapterBuilders.put(JBossLogmanagerMDCAdapter.MDC_CLASS, JBossLogmanagerMDCAdapter::get);
    }

    /**
     * Places the given value under the given key in ALL MDCs of all loaded logging libraries.
     * This change can be undone by invoking the returned Undo object.
     *
     * @param key   the key under which the given value shall be put into all MDCs
     * @param value the value to insert
     *
     * @return A function for undoing the change in all MDCs (Restoring any previously set value).
     */
    public Undo put(String key, String value) {
        List<Undo> undos = new ArrayList<>();
        for (MDCAdapter adapter : enabledAdapters) {
            undos.add(adapter.set(key, value));
        }
        return () -> {
            //iterate in reverse order in case of inter-dependencies
            for (int i = undos.size() - 1; i >= 0; i--) {
                undos.get(i).close();
            }
        };
    }

    @Override
    public synchronized void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        newClasses.stream()
                .filter(clazz -> mdcAdapterBuilders.containsKey(clazz.getName()))
                .filter(clazz -> clazz.getClassLoader() != AgentImpl.INSPECTIT_CLASS_LOADER)
                .filter(clazz -> !(clazz.getClassLoader() instanceof DoNotInstrumentMarker))
                .forEach(clazz -> {
                    try {
                        log.debug("Found MDC implementation for log correlation: {}", clazz.getName());
                        MDCAdapter adapter = mdcAdapterBuilders.get(clazz.getName()).apply(clazz);
                        availableAdapters.put(clazz, adapter);
                        updateEnabledAdaptersSet();
                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }

    @EventListener(InspectitConfigChangedEvent.class)
    @VisibleForTesting
    synchronized void updateEnabledAdaptersSet() {
        TraceIdMDCInjectionSettings settings =
                inspectitEnv.getCurrentConfig().getTracing().getLogCorrelation().getTraceIdMdcInjection();
        enabledAdapters = availableAdapters.values().stream()
                .filter(adapter -> adapter.isEnabledForConfig(settings))
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new WeakHashMap<>())));
    }
}
