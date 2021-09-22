package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.AgentImpl;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.*;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import javax.annotation.PostConstruct;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Manager for handling the creation of {@link MdcAccessor}s for accessing registered MDCs. This is mainly used to
 * inject a trace context into the loggers' MDCs for logging the current trace context.
 */
@Slf4j
@Component
public class MdcAccessManager implements IClassDiscoveryListener {

    @Autowired
    private InspectitEnvironment environment;

    @Autowired
    private ClassInjector classInjector;

    /**
     * All registered {@link MdcAdapter}s. The map's keys represents the FQN class name of the adapter's MDC class.
     */
    private final Map<String, MdcAdapter> mdcAdapters = new HashMap<>();

    /**
     * This map holds references to all available MDC accessors. These accessors have been injected into the related
     * class loaders and are ready to use, but will only be used once they have been activated. This is done so we don't
     * have to scan all existing classes for new MDCs and create accessors once they should be enabled.
     * See also {@link #activeMdcAccessors}.
     * <p>
     * The map uses weak references to the MDC classes as keys. Due to this, once the MDC is gc'ed, the related MDC adapter
     * will be removed from this map because we don't need it anymore.
     */
    @VisibleForTesting
    Map<Class<?>, MdcAccessor> availableMdcAccessors = new WeakHashMap<>();

    /**
     * This collection contains all {@link MdcAccessor}s which are currently active. The collection is using weak
     * references to prevent that the accessor cannot be gc'ed. This is done so the lifetime of the accessor is controlled
     * by the {@link #availableMdcAccessors}, meaning once the underlying MDC is not available anymore, the accessor
     * itself will also be gc'ed.
     */
    @VisibleForTesting
    Collection<MdcAccessor> activeMdcAccessors = Collections.emptySet();

    @PostConstruct
    public void registerAdapters() {
        mdcAdapters.put(Slf4JMdcAdapter.MDC_CLASS, new Slf4JMdcAdapter());
        mdcAdapters.put(Log4J2MdcAdapter.MDC_CLASS, new Log4J2MdcAdapter());
        mdcAdapters.put(Log4J1MdcAdapter.MDC_CLASS, new Log4J1MdcAdapter());
        mdcAdapters.put(JBossLogmanagerMdcAdapter.MDC_CLASS, new JBossLogmanagerMdcAdapter());
    }

    /**
     * Injects the given value under the given key into all activated MDCs.
     *
     * @param key   the key for the value
     * @param value the value to inject
     * @return an {@link InjectionScope} for reverting the injection and restoring the initial MDC state
     */
    public InjectionScope injectValue(String key, String value) {
        List<InjectionScope> scopes = new ArrayList<>();
        for (MdcAccessor mdcAccessor : activeMdcAccessors) {
            InjectionScope scope = mdcAccessor.inject(key, value);
            scopes.add(scope);
        }

        return () -> {
            // iterate in reverse order in case of inter-dependencies between the MDCs
            for (int i = scopes.size(); i-- > 0; ) {
                InjectionScope scope = scopes.get(i);
                scope.close();
            }
        };
    }

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        newClasses.stream()
                .filter(clazz -> mdcAdapters.containsKey(clazz.getName()))
                .filter(clazz -> clazz.getClassLoader() != AgentImpl.INSPECTIT_CLASS_LOADER)
                .filter(clazz -> !(clazz.getClassLoader() instanceof DoNotInstrumentMarker))
                .forEach(clazz -> {
                    try {
                        log.debug("Found MDC implementation for log correlation: {}", clazz.getName());

                        MdcAdapter mdcAdapter = mdcAdapters.get(clazz.getName());

                        MdcAccessor mdcAccessor = createAccessor(mdcAdapter, clazz);

                        availableMdcAccessors.put(clazz, mdcAccessor);

                        updateActiveMdcAccessors();
                    } catch (Throwable t) {
                        if (log.isDebugEnabled()) {
                            log.error("Error creating log-correlation MDC adapter for class '{}', thus trace-id will not be available in the logger.", clazz.getName());
                        } else {
                            log.error("Error creating log-correlation MDC adapter for class '{}', thus trace-id will not be available in the logger.", clazz.getName(), t);
                        }
                    }
                });
    }

    /**
     * Creates a new {@link MdcAccessor} for interacting with the MDC represented by the given class and adapter. The
     * accessor's function and consumers for accessing the MDC will be represented by generated classes which are injected
     * into the MDC's class loader.
     *
     * @param mdcAdapter the adapter specifying the MDC's GET, PUT and REMOVE methods
     * @param mdcClass   the class of the target MDC
     * @return a new {@link MdcAccessor} instance
     * @throws Exception in case the accessor could not be created
     */
    private MdcAccessor createAccessor(MdcAdapter mdcAdapter, Class<?> mdcClass) throws Exception {
        Method getMethod = mdcAdapter.getGetMethod(mdcClass);
        Method putMethod = mdcAdapter.getPutMethod(mdcClass);
        Method removeMethod = mdcAdapter.getRemoveMethod(mdcClass);

        // put consumer
        ClassInjector.ByteCodeProvider bcpPutMethod = createByteCodeProvide(BiConsumer.class, "accept", putMethod);
        Class<? extends BiConsumer<String, Object>> putConsumerClass = injectClass("mdc_bi_consumer", mdcClass, bcpPutMethod);
        BiConsumer<String, Object> putConsumer = putConsumerClass.newInstance();

        // get function
        ClassInjector.ByteCodeProvider bcpGetMethod = createByteCodeProvide(Function.class, "apply", getMethod);
        Class<? extends Function<String, Object>> getFunctionClass = injectClass("mdc_function", mdcClass, bcpGetMethod);
        Function<String, Object> getFunction = getFunctionClass.newInstance();

        // remove consumer
        ClassInjector.ByteCodeProvider bcpRemoveMethod = createByteCodeProvide(Consumer.class, "accept", removeMethod);
        Class<? extends Consumer<String>> removeConsumerClass = injectClass("mdc_consumer", mdcClass, bcpRemoveMethod);
        Consumer<String> removeConsumer = removeConsumerClass.newInstance();

        return mdcAdapter.createAccessor(new WeakReference<>(mdcClass), putConsumer, getFunction, removeConsumer);
    }

    /**
     * Creates a new {@link ClassInjector.ByteCodeProvider} providing the byte code for a generated class. The class itself
     * implements a specific type which defines at least one method. This method will directly invoke to the given target method.
     *
     * @param type             the type of the generated class
     * @param sourceMethodName the name of the method to implement
     * @param targetMethod     the target method which should be invoked once the sourceMethodName is invoked
     * @return a new {@link ClassInjector.ByteCodeProvider} for the generated class
     */
    private ClassInjector.ByteCodeProvider createByteCodeProvide(Class<?> type, String sourceMethodName, Method targetMethod) {
        return className -> new ByteBuddy()
                .subclass(type)
                .name(className)
                .method(named(sourceMethodName)).intercept(MethodCall.invoke(targetMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .getBytes();
    }

    /**
     * Injects the class represented by the given bytecode provider in the class loader of the specified mdc class.
     *
     * @param structureIdentifier unique identifier of the class structure of the generated class
     * @param neighbour           the class will be injected into the class loader of this class
     * @param byteCodeProvider    the provider for the byte-code of the generated class
     * @return the injected class
     * @throws Exception in case the class could not be injected
     */
    private <T> Class<? extends T> injectClass(String structureIdentifier, Class<?> neighbour, ClassInjector.ByteCodeProvider byteCodeProvider) throws Exception {
        InjectedClass<? extends T> injectedClass = (InjectedClass<? extends T>) classInjector.inject(structureIdentifier, neighbour, byteCodeProvider, false);
        return injectedClass.getInjectedClassObject().orElse(null);
    }

    @EventListener(InspectitConfigChangedEvent.class)
    public synchronized void updateActiveMdcAccessors() {
        InspectitConfig config = environment.getCurrentConfig();
        TraceIdMDCInjectionSettings settings = config.getTracing().getLogCorrelation().getTraceIdMdcInjection();

        List<String> previousAccessors = getActiveAccessors();

        this.activeMdcAccessors = this.availableMdcAccessors.values().stream()
                .filter(mdcAccessor -> mdcAccessor.isEnabled(settings))
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new WeakHashMap<>())));

        List<String> activeAccessors = getActiveAccessors();

        activeAccessors.stream()
                .filter(accessor -> !previousAccessors.contains(accessor))
                .forEach(accessor -> log.info("Activated trace-log correlation for MDC '{}'.", accessor));
        previousAccessors.stream()
                .filter(accessor -> !activeAccessors.contains(accessor))
                .forEach(accessor -> log.info("Deactivated trace-log correlation for MDC '{}'.", accessor));
    }

    /**
     * @return a distinct list of FQN class names of MDCs for which there is currently an active {@link MdcAccessor}
     */
    private List<String> getActiveAccessors() {
        return activeMdcAccessors.stream()
                .map(MdcAccessor::getTargetMdcClass)
                .map(Reference::get)
                .filter(Objects::nonNull)
                .map(Class::getName)
                .distinct()
                .collect(Collectors.toList());
    }
}
