package rocks.inspectit.ocelot.core.instrumentation.correlation.log;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.correlation.MdcAccessor;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.core.AgentImpl;
import rocks.inspectit.ocelot.core.MethodTest;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;

import java.lang.reflect.Method;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

@Slf4j
@Component
public class MdcClassListener implements IClassDiscoveryListener {

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        newClasses.stream()
                .filter(clazz -> clazz.getName().equals("org.slf4j.MDC"))
                .filter(clazz -> clazz.getClassLoader() != AgentImpl.INSPECTIT_CLASS_LOADER)
                .filter(clazz -> !(clazz.getClassLoader() instanceof DoNotInstrumentMarker))
                .forEach(clazz -> {
                    try {
                        log.info("Found MDC implementation for log correlation: {}", clazz.getName());

                        Class<? extends MdcAccessor> accessorClass = generateAccessorClass(clazz);
                        MdcAccessor accessor = accessorClass.newInstance();

                        Method get = clazz.getMethod("get", String.class);

                        log.info("get: " + accessor.get("key"));
                        accessor.put("key", "val");
                        log.info("get: " + accessor.get("key"));
                        log.info("get direct: " + get.invoke(null, "key"));
                        accessor.remove("key");
                        log.info("get: " + accessor.get("key"));

                    } catch (Throwable t) {
                        log.error("Error creating log-correlation MDC adapter for class {}", clazz.getName(), t);
                    }
                });
    }

    private Class<? extends MdcAccessor> generateAccessorClass(Class<?> mdcClass) throws NoSuchMethodException {
        ClassLoader targetClassLoader = mdcClass.getClassLoader();

        Method getMethod = mdcClass.getMethod("get", String.class);
        Method putMethod = mdcClass.getMethod("put", String.class, String.class);
        Method removeMethod = mdcClass.getMethod("remove", String.class);

        Class<? extends MdcAccessor> accessorClass = new ByteBuddy()
                .subclass(MdcAccessor.class)
                .method(named("get")).intercept(MethodCall.invoke(getMethod).withAllArguments())
//                .method(named("get")).intercept(MethodDelegation.to(mdcClass))
                .method(named("put")).intercept(MethodCall.invoke(putMethod).withAllArguments())
                .method(named("remove")).intercept(MethodCall.invoke(removeMethod).withAllArguments())
                .make()
                .load(targetClassLoader)
                .getLoaded();

        return accessorClass;
    }
}
