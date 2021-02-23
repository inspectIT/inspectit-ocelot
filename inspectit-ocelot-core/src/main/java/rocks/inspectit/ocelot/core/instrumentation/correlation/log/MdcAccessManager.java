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
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.named;

@Slf4j
@Component
public class MdcAccessManager implements IClassDiscoveryListener {

    @Autowired
    private InspectitEnvironment environment;

    @Autowired
    private ClassInjector classInjector;

    private final Map<String, MdcAdapter> mdcAdapters = new HashMap<>();

    @VisibleForTesting
    Map<Class<?>, DelegationMdcAccessor> availableMdcAccessors = new WeakHashMap<>();

    /**
     * weak!
     */
    @VisibleForTesting
    Collection<DelegationMdcAccessor> activeMdcAccessors = Collections.emptySet();

    @PostConstruct
    public void registerAdapters() {
        mdcAdapters.put(Slf4JMdcAdapter.MDC_CLASS, new Slf4JMdcAdapter());
        mdcAdapters.put(Log4J2MdcAdapter.MDC_CLASS, new Log4J2MdcAdapter());
        mdcAdapters.put(Log4J1MdcAdapter.MDC_CLASS, new Log4J1MdcAdapter());
        mdcAdapters.put(JBossLogmanagerMdcAdapter.MDC_CLASS, new JBossLogmanagerMdcAdapter());
        mdcAdapters.put(TestMdcAdapter.MDC_CLASS, new TestMdcAdapter());
    }

    public InjectionScope injectValue(String key, String value) {
        List<InjectionScope> scopes = new ArrayList<>();
        for (DelegationMdcAccessor mdcAccessor : activeMdcAccessors) {
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
                        log.info("Found MDC implementation for log correlation: {}", clazz.getName());

                        MdcAdapter mdcAdapter = mdcAdapters.get(clazz.getName());

                        DelegationMdcAccessor mdcAccessor = createAccessor(mdcAdapter, clazz);

                        availableMdcAccessors.put(clazz, mdcAccessor);

                        updateActiveMdcAccessors();
                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }

    private DelegationMdcAccessor createAccessor(MdcAdapter mdcAdapter, Class<?> mdcClass) throws Exception {
        Method getMethod = mdcAdapter.getGetMethod(mdcClass);
        Method putMethod = mdcAdapter.getPutMethod(mdcClass);
        Method removeMethod = mdcAdapter.getRemoveMethod(mdcClass);

        // put
        ClassInjector.ByteCodeProvider bcpPutMethod = createByteCodeProvide(BiConsumer.class, "accept", putMethod);
        Class<? extends BiConsumer<String, Object>> putConsumerClass = injectClass("mdc_bi_consumer", mdcClass, bcpPutMethod);
        BiConsumer<String, Object> putConsumer = putConsumerClass.newInstance();

        // get
        ClassInjector.ByteCodeProvider bcpGetMethod = createByteCodeProvide(Function.class, "apply", getMethod);
        Class<? extends Function<String, Object>> getFunctionClass = injectClass("mdc_function", mdcClass, bcpGetMethod);
        Function<String, Object> getFunction = getFunctionClass.newInstance();

        // remove
        ClassInjector.ByteCodeProvider bcpRemoveMethod = createByteCodeProvide(Consumer.class, "accept", removeMethod);
        Class<? extends Consumer<String>> removeConsumerClass = injectClass("mdc_consumer", mdcClass, bcpRemoveMethod);
        Consumer<String> removeConsumer = removeConsumerClass.newInstance();

        return mdcAdapter.wrap(putConsumer, getFunction, removeConsumer);
    }

    private ClassInjector.ByteCodeProvider createByteCodeProvide(Class<?> type, String sourceMethodName, Method targetMethod) {
        return className -> new ByteBuddy()
                .subclass(type)
                .name(className)
                .method(named(sourceMethodName)).intercept(MethodCall.invoke(targetMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .getBytes();
    }

    private <T> Class<? extends T> injectClass(String structureIdentifier, Class<?> mdcClass, ClassInjector.ByteCodeProvider byteCodeProvider) throws Exception {
        InjectedClass<? extends T> injectedClass = (InjectedClass<? extends T>) classInjector.inject(structureIdentifier, mdcClass, byteCodeProvider, false);
        return injectedClass.getInjectedClassObject().get();
    }

    @EventListener(InspectitConfigChangedEvent.class)
    public synchronized void updateActiveMdcAccessors() {
        InspectitConfig config = environment.getCurrentConfig();
        TraceIdMDCInjectionSettings settings = config.getTracing().getLogCorrelation().getTraceIdMdcInjection();

//        Collection<DelegationMdcAccessor> previousAccessors = this.activeMdcAccessors;

        this.activeMdcAccessors = this.availableMdcAccessors.values().stream()
                .filter(mdcAccessor -> mdcAccessor.isEnabled(settings))
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new WeakHashMap<>())));
//
//        previousAccessors.keySet().stream()
//                .filter(className -> !this.activeMdcAccessors.contains(className))
//                .distinct()
//                .forEach(className -> log.info("Disabled log-correlation for class: {}", className));
    }
}
