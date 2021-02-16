package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.tracing.TraceIdMDCInjectionSettings;
import rocks.inspectit.ocelot.core.AgentImpl;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.*;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.named;

@Slf4j
@Component
public class MdcAccessManager implements IClassDiscoveryListener {

    @Autowired
    private InspectitEnvironment environment;

    private Map<String, MdcAdapter> mdcAdapters = new HashMap<>();

    @VisibleForTesting
    Map<Class<?>, DelegationMdcAccessor> availableMdcAccessors = new WeakHashMap();

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

                        Class<? extends MdcAccessor> accessorClass = injectAccessorClass(mdcAdapter, clazz);
                        MdcAccessor accessor = accessorClass.newInstance();

                        availableMdcAccessors.put(clazz, mdcAdapter.wrap(accessor));

                        updateActiveMdcAccessors();
                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }

    private Class<? extends MdcAccessor> injectAccessorClass(MdcAdapter mdcAdapter, Class<?> mdcClass) throws NoSuchMethodException {
        ClassLoader mdcClassLoader = mdcClass.getClassLoader();

        Method getMethod = mdcAdapter.getGetMethod(mdcClass);
        Method putMethod = mdcAdapter.getPutMethod(mdcClass);
        Method removeMethod = mdcAdapter.getRemoveMethod(mdcClass);

        DynamicType.Unloaded<MdcAccessor> dynamicType = new ByteBuddy()
                .subclass(MdcAccessor.class)
                .method(named("get")).intercept(MethodCall.invoke(getMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(named("put")).intercept(MethodCall.invoke(putMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(named("remove")).intercept(MethodCall.invoke(removeMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make();

        return dynamicType
                .load(mdcClassLoader)
                .getLoaded();
    }

    @EventListener(InspectitConfigChangedEvent.class)
    public synchronized void updateActiveMdcAccessors() {
        InspectitConfig config = environment.getCurrentConfig();
        TraceIdMDCInjectionSettings settings = config.getTracing().getLogCorrelation().getTraceIdMdcInjection();

        activeMdcAccessors = availableMdcAccessors.values().stream()
                .filter(mdcAccessor -> mdcAccessor.isEnabled(settings))
                .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new WeakHashMap<>())));
    }
}
