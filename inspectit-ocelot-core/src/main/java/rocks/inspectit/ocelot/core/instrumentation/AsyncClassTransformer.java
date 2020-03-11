package rocks.inspectit.ocelot.core.instrumentation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.ocelot.core.instrumentation.event.TransformerShutdownEvent;
import rocks.inspectit.ocelot.core.instrumentation.hook.DispatchHookAdvices;
import rocks.inspectit.ocelot.core.instrumentation.injection.JigsawModuleInstrumenter;
import rocks.inspectit.ocelot.core.instrumentation.special.ClassLoaderDelegation;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
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
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    @VisibleForTesting
    List<IClassDefinitionListener> classDefinitionListeners;

    @Autowired
    private JigsawModuleInstrumenter moduleManager;

    @Autowired
    private ClassLoaderDelegation classLoaderDelegation;

    /**
     * Detects if the instrumenter is in the process of shutting down.
     * When it is shutting down, no new instrumentations are added anymore, instead all existing instrumentations are removed.
     */
    @Getter
    private volatile boolean shuttingDown = false;

    /**
     * A lock for safely accessing {@link #shuttingDown} in combination with {@link #instrumentedClasses}.
     */
    private final Object shutDownLock = new Object();

    /**
     * Stores all classes which have been instrumented with a configuration different
     * than {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION}.
     * <p>
     * Package private for testing.
     */
    Cache<Class<?>, Boolean> instrumentedClasses = CacheBuilder.newBuilder().weakKeys().build();


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        if (classBeingRedefined == null) { // class is not loaded yet! we only redefine only loaded classes to prevent blocking
            classDefinitionListeners.forEach(lis -> lis.onNewClassDefined(className, loader));
            return bytecode; //leave the class unchanged for now

        }
        //retransforms can be triggered by other agents where the classloader delegation has not been applied yet
        if (!classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(loader, configResolver.getCurrentConfig()).isEmpty()) {
            log.debug("Skipping instrumentation of {} as bootstrap classes were not made available yet for the class", className);
            return bytecode; //leave the class unchanged for now
        } else {
            return applyInstrumentation(classBeingRedefined, bytecode);
        }
    }

    @PostConstruct
    void init() {
        instrumentation.addTransformer(this, true);
    }

    /**
     * Removes all applied instrumentations if the JVM is not shutting down but the agent is.
     */
    @PreDestroy
    void destroy() {
        // this lock guarantees through updateAndGetActiveConfiguration that no instrumentation is added after the lock is released
        synchronized (shutDownLock) {
            shuttingDown = true;
        }
        ctx.publishEvent(new TransformerShutdownEvent(this));
        if (!CoreUtils.isJVMShuttingDown()) {
            deinstrumentAllClasses();
        }
        instrumentation.removeTransformer(this);
    }

    /**
     * Triggers a retransformation for all instrumented classes until none is instrumented anymore.
     * Therefore this class expects that {@link #shuttingDown} is already set to true.
     * When {@link #shuttingDown} is true, for every retransformed class any instrumentation is removed automatically.
     */
    private void deinstrumentAllClasses() {
        //we deinstrument classloaders last to prevent classloading issues with the classloader delegation
        Set<Class<?>> instrumentedClassLoaders = new HashSet<>();

        //elements are removed from instrumentedClasses by the updateAndGetActiveConfiguration method
        while (instrumentedClasses.size() > instrumentedClassLoaders.size()) {
            List<Class<?>> batchClasses = new ArrayList<>();
            int maxBatchSize = env.getCurrentConfig().getInstrumentation().getInternal().getClassRetransformBatchSize();

            Iterator<Class<?>> it = instrumentedClasses.asMap().keySet().iterator();
            while (batchClasses.size() < maxBatchSize && it.hasNext()) {
                Class<?> clazz = it.next();
                if (ClassLoader.class.isAssignableFrom(clazz)) {
                    instrumentedClassLoaders.add(clazz);
                } else {
                    batchClasses.add(clazz);
                }
            }
            removeInstrumentationForBatch(batchClasses);
        }
        if (instrumentedClassLoaders.size() > 0) {
            removeInstrumentationForBatch(new ArrayList<>(instrumentedClassLoaders));
        }
    }

    private void removeInstrumentationForBatch(List<Class<?>> batchClasses) {
        if (!batchClasses.isEmpty()) {
            try {
                instrumentation.retransformClasses(batchClasses.toArray(new Class[]{}));
                Thread.yield(); //yield to allow the target application to do stuff between the batches
            } catch (Exception e) {
                if (batchClasses.size() > 1) {
                    log.error("Error removing applied transformations for batch, retrying classes one by one", e);
                    for (Class<?> clazz : batchClasses) {
                        try {
                            instrumentation.retransformClasses(clazz);
                        } catch (Exception e2) {
                            log.error("Error removing applied transformations of class '{}'", clazz.getName(), e2);
                            instrumentedClasses.invalidate(clazz);
                        }
                    }
                } else {
                    Class<?> clazz = batchClasses.get(0);
                    log.error("Error removing applied transformations of class '{}'", clazz.getName(), e);
                    instrumentedClasses.invalidate(clazz);
                }
            }
        }
    }

    private byte[] applyInstrumentation(Class<?> classBeingRedefined, byte[] originalByteCode) {
        try {
            //load the type description and the desired instrumentation
            TypeDescription type = TypeDescription.ForLoadedType.of(classBeingRedefined);
            ClassInstrumentationConfiguration classConf = updateAndGetActiveConfiguration(classBeingRedefined, type);

            byte[] resultBytes;
            if (classConf.isNoInstrumentation()) {
                // we do not want to instrument this -> we return the original byte code
                resultBytes = originalByteCode;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Redefining class: {}", type.getName());
                }
                moduleManager.openModule(classBeingRedefined);

                //Make a ByteBuddy builder based on the input bytecode
                ClassFileLocator byteCodeClassFileLocator = ClassFileLocator.Simple.of(type.getName(), originalByteCode);
                DynamicType.Builder<?> builder = new ByteBuddy().redefine(classBeingRedefined, byteCodeClassFileLocator);

                //Apply the actual instrumentation onto the builders
                for (SpecialSensor specialSensor : classConf.getActiveSpecialSensors()) {
                    builder = specialSensor.instrument(classBeingRedefined, classConf.getActiveConfiguration(), builder);
                }

                // Apply the instrumentation hook
                ElementMatcher.Junction<MethodDescription> methodMatcher = getCombinedMethodMatcher(classBeingRedefined, classConf);
                if (methodMatcher != null) {
                    builder = DispatchHookAdvices.adviceOn(builder, methodMatcher);
                }

                //"Compile" the builder to bytecode
                DynamicType.Unloaded<?> instrumentedClass = builder.make();
                resultBytes = instrumentedClass.getBytes();
            }

            //Notify listeners that this class has been instrumented (or deinstrumented)
            val event = new ClassInstrumentedEvent(this, classBeingRedefined, type, classConf);

            if (!shuttingDown) {
                ctx.publishEvent(event);
            }

            return resultBytes;
        } catch (Exception e) {
            log.error("Error generating instrumented bytecode", e);
            return originalByteCode;
        }
    }

    /**
     * Combining all method matchers of the matching rules in order to prevent multiple injections of the advice.
     */
    private ElementMatcher.Junction<MethodDescription> getCombinedMethodMatcher(Class<?> clazz, ClassInstrumentationConfiguration classConfig) {
        ElementMatcher.Junction<MethodDescription> methodMatcher = null;

        for (InstrumentationRule rule : classConfig.getActiveRules()) {
            if (log.isDebugEnabled()) {
                log.debug("Added hook to {} due to rule '{}'.", clazz, rule.getName());
            }
            for (InstrumentationScope scope : rule.getScopes()) {
                if (log.isTraceEnabled()) {
                    log.trace("|> {}", scope.getTypeMatcher());
                }

                if (methodMatcher == null) {
                    methodMatcher = scope.getMethodMatcher();
                } else {
                    methodMatcher = methodMatcher.or(scope.getMethodMatcher());
                }
            }
        }

        return methodMatcher;
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
        ClassInstrumentationConfiguration classConf = configResolver.getClassInstrumentationConfiguration(classBeingRedefined);

        synchronized (shutDownLock) {
            if (shuttingDown) {
                classConf = ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
            }
            if (classConf.isNoInstrumentation()) {
                if (instrumentedClasses.getIfPresent(classBeingRedefined) != null) {
                    log.debug("Removing instrumentation of {}", classBeingRedefined);
                    instrumentedClasses.invalidate(classBeingRedefined);
                }
            } else {
                log.debug("Applying instrumentation of {}", classBeingRedefined);
                instrumentedClasses.put(classBeingRedefined, Boolean.TRUE);
            }
        }
        selfMonitorInstrumentedClassesCount();
        return classConf;
    }


    @EventListener(classes = {InspectitConfigChangedEvent.class},
            condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorInstrumentedClassesCount() {
        selfMonitoring.recordMeasurement("instrumented-classes", instrumentedClasses.size());
    }

}
