package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.core.AgentImpl;
import rocks.inspectit.ocelot.core.instrumentation.correlation.log.adapters.*;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

@Slf4j
@Component
public class MdcAccessManager implements IClassDiscoveryListener {

    /**
     * Non-throwing closeable.
     */
    public interface RevertTraceInjection extends AutoCloseable {

        @Override
        void close();

        /**
         * A no-operation RevertTraceInjection.
         */
        RevertTraceInjection NOOP = () -> {
        };
    }

    private Map<String, AbstractMdcAccessor> mdcAdapters = new HashMap<>();

    @Getter
    private Map<Class<?>, MdcAccessor> mdcAccessors = new WeakHashMap();

    @VisibleForTesting
    @PostConstruct
    void registerAdapters() {
        mdcAdapters.put(Slf4JMdcAccessor.MDC_CLASS, new Slf4JMdcAccessor());
//        mdcAdapters.put(Slf4JMdcAdapter.MDC_CLASS, new Slf4JMdcAdapter());
//        mdcAdapters.put(Log4J2MdcAdapter.MDC_CLASS, new Log4J2MdcAdapter());
//        mdcAdapters.put(Log4J1MdcAdapter.MDC_CLASS, new Log4J1MdcAdapter());
//        mdcAdapters.put(JBossLogmanagerMdcAdapter.MDC_CLASS, new JBossLogmanagerMdcAdapter());
    }

//    public RevertTraceInjection put(String key, String value) {
//        List<RevertTraceInjection> reverts = new ArrayList<>();
//        for (MdcAccessor mdcAccessor : mdcAccessors) {
//
//            undos.add(adapter.set(key, value));
//        }
//        return () -> {
//            //iterate in reverse order in case of inter-dependencies
//            for (int i = undos.size() - 1; i >= 0; i--) {
//                undos.get(i).close();
//            }
//        };
//    }
//
//    private RevertTraceInjection put(MdcAccessor mdcAccessor, String key, String value) {
//        Method put = putMethod.get();
//        Method get = getMethod.get();
//        Method remove = removeMethod.get();
//
//        if (put == null || get == null || remove == null) {
//            return MDCAccess.Undo.NOOP; //the MDC has been garbage collected
//        }
//
//        try {
//
//            Object previous = get.invoke(null, key);
//            if (value != null) {
//                put.invoke(null, key, value);
//            } else {
//                remove.invoke(null, key);
//            }
//
//            return () -> {
//                try {
//                    if (previous != null) {
//                        put.invoke(null, key, previous);
//                    } else {
//                        remove.invoke(null, key);
//                    }
//                } catch (Throwable e) {
//                    log.error("Could not reset MDC", e);
//                }
//            };
//        } catch (Throwable e) {
//            log.error("Could not write to MDC", e);
//            return MDCAccess.Undo.NOOP;
//        }
//    }

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        newClasses.stream()
                .filter(clazz -> mdcAdapters.containsKey(clazz.getName()))
                .filter(clazz -> clazz.getClassLoader() != AgentImpl.INSPECTIT_CLASS_LOADER)
                .filter(clazz -> !(clazz.getClassLoader() instanceof DoNotInstrumentMarker))
                .forEach(clazz -> {
                    try {
                        log.info("Found MDC implementation for log correlation: {}", clazz.getName());

                        Class<? extends MdcAccessor> accessorClass = injectAccessorClass(clazz);
                        MdcAccessor accessor = accessorClass.newInstance();

                        mdcAccessors.put(clazz, accessor);

                        Method get = clazz.getMethod("get", String.class);

                        log.info("get: " + accessor.get("key"));
                        accessor.put("key", "val");
                        log.info("get: " + accessor.get("key"));
                        log.info("get direct: " + get.invoke(null, "key"));
                        accessor.remove("key");
                        log.info("get: " + accessor.get("key"));

                        try (AutoCloseable c = accessor.inject("test", "va")){
                            log.info("get: " + accessor.get("test"));
                        }
                        log.info("get: " + accessor.get("test"));

                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }

    private Class<? extends MdcAccessor> injectAccessorClass(Class<?> mdcClass) throws NoSuchMethodException {
        ClassLoader mdcClassLoader = mdcClass.getClassLoader();

        AbstractMdcAccessor mdcAdapter = mdcAdapters.get(mdcClass.getName());

        Method getMethod = mdcAdapter.getGetMethod(mdcClass);
        Method putMethod = mdcAdapter.getPutMethod(mdcClass);
        Method removeMethod = mdcAdapter.getRemoveMethod(mdcClass);

//        return new ByteBuddy()
//                .subclass(MdcAccessor.class)
//                .method(named("get")).intercept(MethodCall.invoke(getMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
//                .method(named("put")).intercept(MethodCall.invoke(putMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
//                .method(named("remove")).intercept(MethodCall.invoke(removeMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
//                .implement(DoNotInstrumentMarker.class)
//                .make()
//                .load(mdcClassLoader)
//                .getLoaded();
        return new ByteBuddy()
                .subclass(mdcAdapter.getClass())
                .method(named("get")).intercept(MethodCall.invoke(getMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(named("put")).intercept(MethodCall.invoke(putMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(named("remove")).intercept(MethodCall.invoke(removeMethod).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .load(mdcClassLoader)
                .getLoaded();
    }
}
