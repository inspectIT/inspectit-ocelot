package rocks.inspectit.oce.core.instrumentation;

import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

/**
 * A class transformer applying all inspectIT instrumentations.
 * This transform only instrument classes when they are redefined / retransformed!.
 */
@Component
@Slf4j
public class AsyncClassTransformer implements ClassFileTransformer {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private IgnoredClassesManager ignoredClasses;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private List<IClassDefinitionListener> classDefinitionListeners;

    /**
     * Detects if the instrumenter is in the process of shutting down.
     * When it is shutting down, no new instrumentations are added anymore, instead all existing instrumentations are removed.
     */
    @Getter
    private volatile boolean shuttingDown = false;

    /**
     * A lock for safely accessing {@link #shuttingDown} in combination with {@link #instrumentedClasses}.
     */
    private Object shutDownLock = new Object();

    /**
     * Stores all classes which have been instrumented with a configuration different
     * than {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION}.
     */
    private Set<Class<?>> instrumentedClasses = Collections.newSetFromMap(
            CacheBuilder.newBuilder().weakKeys().<Class<?>, Boolean>build().asMap());


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        if (classBeingRedefined == null) { // class is not loaded yet! we only redefine only loaded classes to prevent blocking
            classDefinitionListeners.forEach(lis -> lis.newClassDefined(className, loader));
            return bytecode; //leave the class unchanged for now
        } else {
            return applyInstrumentation(classBeingRedefined, bytecode);
        }
    }

    @PostConstruct
    private void init() {
        instrumentation.addTransformer(this, true);
    }

    /**
     * Removes all applied instrumentations if the JVM is not shutting down but the agent is.
     */
    @PreDestroy
    private void destroy() {
        // this look guarantees through updateAndGetActiveConfiguration that no instrumentation is added after the lock is released
        synchronized (shutDownLock) {
            shuttingDown = true;
        }
        if (!CommonUtils.isJVMShuttingDown()) {
            deinstrumentAllClasses();
        }
        instrumentation.removeTransformer(this);
    }

    private void deinstrumentAllClasses() {
        while (!instrumentedClasses.isEmpty()) {
            List<Class<?>> batchClasses = new ArrayList<>();
            int maxBatchSize = env.getCurrentConfig().getInstrumentation().getInternal().getClassRetransformBatchSize();

            Iterator<Class<?>> it = instrumentedClasses.iterator();
            for (int i = 0; i < maxBatchSize && it.hasNext(); i++) {
                batchClasses.add(it.next());
            }

            try {
                instrumentation.retransformClasses(batchClasses.toArray(new Class[]{}));
            } catch (UnmodifiableClassException e) {
                log.error("Error removing applied transformations", e);
            }
        }
    }

    private byte[] applyInstrumentation(Class<?> classBeingRedefined, byte[] originalByteCode) {
        try {
            TypeDescription type = TypeDescription.ForLoadedType.of(classBeingRedefined);

            ClassInstrumentationConfiguration classConf = updateAndGetActiveConfiguration(classBeingRedefined, type);

            byte[] resultBytes;
            if (classConf.isSameAs(type, ClassInstrumentationConfiguration.NO_INSTRUMENTATION)) {
                resultBytes = originalByteCode;
            } else {
                ClassFileLocator byteCodeClassFileLocator = ClassFileLocator.Simple.of(classBeingRedefined.getName(), originalByteCode);
                DynamicType.Builder<?> parsed = new ByteBuddy().redefine(classBeingRedefined, byteCodeClassFileLocator);

                for (SpecialSensor specs : classConf.getActiveSpecialSensors()) {
                    parsed = specs.instrument(classBeingRedefined, type, classConf.getActiveConfiguration(), parsed);
                }
                DynamicType.Unloaded<?> made = parsed.make();
                resultBytes = made.getBytes();
            }


            val event = new ClassInstrumentedEvent(this, classBeingRedefined, type, classConf);
            ctx.publishEvent(event);

            return resultBytes;
        } catch (Exception e) {
            log.error("Error generating instrumented bytecode", e);
            return originalByteCode;
        }
    }

    /**
     * Derives the {@link ClassInstrumentationConfiguration} based on the latest environment configuration for a given type.
     * In addition the class is added to {@link #instrumentedClasses} if it is instrumented or removed from the set otherwise.
     * If the class transformer is shutting down, this returns {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION}
     * for every class (= all classes should be deinstrumented).
     *
     * @param classBeingRedefined the class to check for
     * @param type                the classes type description
     * @return
     */
    private ClassInstrumentationConfiguration updateAndGetActiveConfiguration(Class<?> classBeingRedefined, TypeDescription type) {
        val instrConf = env.getCurrentConfig().getInstrumentation();
        ClassInstrumentationConfiguration classConf = ClassInstrumentationConfiguration.getFor(type, instrConf, specialSensors);

        synchronized (shutDownLock) {
            if (shuttingDown || ignoredClasses.isIgnoredClass(classBeingRedefined)) {
                classConf = ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
            }
            if (classConf.isSameAs(type, ClassInstrumentationConfiguration.NO_INSTRUMENTATION)) {
                log.debug("Removing instrumentation of {}", classBeingRedefined);
                instrumentedClasses.remove(classBeingRedefined);
            } else {
                log.debug("Applying instrumentation of {}", classBeingRedefined);
                instrumentedClasses.add(classBeingRedefined);
            }
        }
        return classConf;
    }

}
