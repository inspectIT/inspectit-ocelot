package rocks.inspectit.oce.core.instrumentation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.utils.CommonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@Component
@Slf4j
public class AsyncClassTransformer implements ClassFileTransformer {
    /**
     * These classes are ignored when found on the bootstrap.
     * Those are basically the inspectIT classes and OpenCensus with its dependencies
     */
    private static final List<String> IGNORED_BOOTSTRAP_PACKAGES = Arrays.asList(
            "rocks.inspectit.",
            "io.opencensus.",
            "io.grpc.",
            "com.lmax.disruptor.",
            "com.google."
    );

    private static final ClassLoader INSPECTIT_CLASSLOADER = AsyncClassTransformer.class.getClassLoader();

    @Autowired
    InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private List<SpecialSensor> sensors;

    @Getter
    private boolean shuttingDown = false;

    private Object shutDownLock = new Object();

    private Set<BiConsumer<Class<?>, ClassInstrumentationConfiguration>> postInstrumentationListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * If a class is not present here, it means it was not instrumented
     */
    private Map<Class<?>, ClassInstrumentationConfiguration> activeConfigurations = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Specifies the most recent time stamp when transform wasCalled for an initial definition of a class.
     */
    @Getter
    private AtomicLong lastNewClassDefinitionTimestamp = new AtomicLong(0);


    public void addPostInstrumentationListener(BiConsumer<Class<?>, ClassInstrumentationConfiguration> listener) {
        postInstrumentationListeners.add(listener);
    }


    public boolean isIgnoredClass(Class<?> clazz) {
        if (clazz.getClassLoader() == INSPECTIT_CLASSLOADER) {
            return true;
        } else if (clazz.getClassLoader() == null) {
            String name = clazz.getName();
            return IGNORED_BOOTSTRAP_PACKAGES.stream()
                    .filter(prefix -> name.startsWith(prefix))
                    .findAny().isPresent()
                    || !instrumentation.isModifiableClass(clazz);
        }
        return !instrumentation.isModifiableClass(clazz);
    }

    public ClassInstrumentationConfiguration getActiveClassInstrumentationConfiguration(Class<?> clazz) {
        return activeConfigurations.getOrDefault(clazz, ClassInstrumentationConfiguration.NO_INSTRUMENTATION);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        if (classBeingRedefined == null) { // class is not loaded yet! we only redefine only loaded classes to prevent blocking
            lastNewClassDefinitionTimestamp.set(System.currentTimeMillis());
            return bytecode; //leave the class unchanged for now
        } else {
            if (isIgnoredClass(classBeingRedefined)) {
                return bytecode;
            } else {
                return applyInstrumentation(classBeingRedefined, bytecode);
            }
        }
    }

    @PostConstruct
    private void init() {
        instrumentation.addTransformer(this, true);
    }

    @PreDestroy
    private void destory() {
        synchronized (shutDownLock) {
            shuttingDown = true;
        }
        if (CommonUtils.isJVMShuttingDown()) {

            while (!activeConfigurations.isEmpty()) {
                try {
                    instrumentation.retransformClasses(activeConfigurations.keySet().toArray(new Class[]{}));
                } catch (UnmodifiableClassException e) {
                    log.error("Error removing applied transformations", e);
                }
            }
            instrumentation.removeTransformer(this);
        }
    }


    private byte[] applyInstrumentation(Class<?> classBeingRedefined, byte[] bytecode) {
        try {
            val instrConf = env.getCurrentConfig().getInstrumentation();
            TypeDescription type = TypeDescription.ForLoadedType.of(classBeingRedefined);

            ClassInstrumentationConfiguration classConf;
            synchronized (shutDownLock) {
                if (shuttingDown) {
                    classConf = ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
                } else {
                    classConf = ClassInstrumentationConfiguration.getFor(type, instrConf, sensors);
                }
                if (classConf.isSameAs(type, ClassInstrumentationConfiguration.NO_INSTRUMENTATION)) {
                    log.debug("Removing instrumentation of {}", classBeingRedefined);
                    activeConfigurations.remove(classBeingRedefined);
                } else {
                    log.debug("Applying instrumentation of {}", classBeingRedefined);
                    activeConfigurations.put(classBeingRedefined, classConf);
                }
            }


            DynamicType.Builder<?> parsed = new ByteBuddy().redefine(classBeingRedefined,
                    ClassFileLocator.Simple.of(classBeingRedefined.getName(), bytecode));
            for (SpecialSensor specs : classConf.getActiveSensors()) {
                parsed = specs.instrument(type, classConf.getActiveConfiguration(), parsed);
            }
            DynamicType.Unloaded<?> made = parsed.make();
            byte[] bytes = made.getBytes();

            postInstrumentationListeners.forEach(lis -> lis.accept(classBeingRedefined, classConf));

            return bytes;
        } catch (Exception e) {
            log.error("Error generating instrumented bytecode", e);
        }
        return bytecode;
    }

}
