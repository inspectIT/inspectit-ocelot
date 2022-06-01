package rocks.inspectit.ocelot.core.instrumentation.transformer;

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
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.instrumentation.event.TransformerShutdownEvent;
import rocks.inspectit.ocelot.core.instrumentation.hook.DispatchHookAdvices;
import rocks.inspectit.ocelot.core.instrumentation.injection.JigsawModuleInstrumenter;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Base for all classes which should act as {@link java.lang.instrument.ClassFileTransformer}s. Anyway, we instroduced
 * a custom {@link ClassTransformer} interface to ease Java8 and Java9 interoperability.
 */
@Slf4j
public abstract class AbstractClassTransformer implements ClassTransformer {

    /**
     * A lock for safely accessing {@link #shuttingDown} in combination with {@link #instrumentedClasses}.
     */
    protected final Object shutDownLock = new Object();

    @Autowired
    protected ApplicationContext ctx;

    @Autowired
    protected Instrumentation instrumentation;

    @Autowired
    protected InstrumentationConfigurationResolver configResolver;

    @Autowired
    protected InspectitEnvironment env;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private JigsawModuleInstrumenter moduleManager;

    /**
     * Detects if the instrumenter is in the process of shutting down.
     * When it is shutting down, no new instrumentations are added anymore, instead all existing instrumentations are removed.
     */
    @Getter
    protected volatile boolean shuttingDown = false;

    /**
     * Stores all classes which have been instrumented with a configuration different
     * form {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION}.
     * <p>
     * Package private for testing.
     */
    Cache<Class<?>, Boolean> instrumentedClasses = CacheBuilder.newBuilder().weakKeys().build();

    @Override
    public void destroy() {
        // this lock guarantees through updateAndGetActiveConfiguration that no instrumentation is added after the lock is released
        synchronized (shutDownLock) {
            shuttingDown = true;
        }
        ctx.publishEvent(new TransformerShutdownEvent(this));
        if (!CoreUtils.isJVMShuttingDown()) {
            deinstrumentAllClasses();
        }
    }

    @Override
    public byte[] transform(Object module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String classNameInDotNotation = className.replace("/", ".");
        if (InstrumentationConfigurationResolver.isClassFromIgnoredPackage(env.getCurrentConfig()
                .getInstrumentation(), classNameInDotNotation, loader)) {
            return classfileBuffer;
        }
        if (module != null) {
            moduleManager.openModule(module);
        }
        return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String classNameInDotNotation = className.replace("/", ".");
        if (InstrumentationConfigurationResolver.isClassFromIgnoredPackage(env.getCurrentConfig()
                .getInstrumentation(), classNameInDotNotation, loader)) {
            return classfileBuffer;
        }
        return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }

    /**
     * Entry point for subclasses to implemented their transformation
     */
    public abstract byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException;

    /**
     * Applies the {@link ClassInstrumentationConfiguration} to the provided bytecode. If modification fails, uninstrumented bytecode is returned.
     *
     * @param typeWithLoader The {@link TypeDescriptionWithClassLoader} representing this class to be instrumented
     * @param bytecode       The bytecode to by modified
     * @param classConf      The {@link ClassInstrumentationConfiguration} for the class to be instrumented
     *
     * @return The modified bytecode
     */
    protected byte[] instrumentByteCode(TypeDescriptionWithClassLoader typeWithLoader, Class<?> classBeingRedefined, byte[] bytecode, ClassInstrumentationConfiguration classConf) {
        try {
            if (classConf.isNoInstrumentation()) {
                return bytecode;
            }

            if (log.isDebugEnabled()) {
                log.debug("Redefining class: {}", typeWithLoader.getName());
            }

            // Make a ByteBuddy builder based on the input bytecode
            ClassFileLocator bytecodeClassFileLocator = ClassFileLocator.Simple.of(typeWithLoader.getName(), bytecode);
            // See https://github.com/raphw/byte-buddy/issues/1095 and https://github.com/raphw/byte-buddy/issues/1040
            // why we are using the different method graph and decorate function here
            DynamicType.Builder<?> builder = new ByteBuddy().with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE)
                    .decorate(typeWithLoader.getType(), bytecodeClassFileLocator);

            // Apply the actual instrumentation onto the builders
            for (SpecialSensor specialSensor : classConf.getActiveSpecialSensors()) {
                builder = specialSensor.instrument(typeWithLoader, classConf.getActiveConfiguration(), builder);
            }

            // Apply the instrumentation hook
            ElementMatcher.Junction<MethodDescription> methodMatcher = getCombinedMethodMatcher(typeWithLoader.getType(), classConf);
            if (methodMatcher != null) {
                builder = DispatchHookAdvices.adviceOn(builder, methodMatcher);
            }

            // "Compile" the builder to bytecode
            DynamicType.Unloaded<?> instrumentedClass = builder.make();

            if (classBeingRedefined != null) {
                dispatchClassInstrumentedEvent(classBeingRedefined, typeWithLoader.getType(), classConf);
            }

            return instrumentedClass.getBytes();
        } catch (Throwable throwable) {
            log.warn("Could not instrument class '{}' due to an error during bytecode generation.", typeWithLoader.getName(), throwable);
            return bytecode;
        }
    }

    protected void dispatchClassInstrumentedEvent(Class<?> clazz, TypeDescription type, ClassInstrumentationConfiguration classConf) {
        if (!shuttingDown) {
            //Notify listeners that this class has been instrumented (or deinstrumented)
            val event = new ClassInstrumentedEvent(this, clazz, type, classConf);
            ctx.publishEvent(event);
        }
    }

    /**
     * Triggers a retransformation for all instrumented classes until none is instrumented anymore.
     * Therefore this class expects that {@link #shuttingDown} is already set to true.
     * When {@link #shuttingDown} is true, for every retransformed class any instrumentation is removed automatically.
     */
    protected void deinstrumentAllClasses() {
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
        if (!instrumentedClassLoaders.isEmpty()) {
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

    /**
     * Combining all method matchers of the matching rules in order to prevent multiple injections of the advice.
     */
    private ElementMatcher.Junction<MethodDescription> getCombinedMethodMatcher(TypeDescription type, ClassInstrumentationConfiguration classConfig) {
        ElementMatcher.Junction<MethodDescription> methodMatcher = null;

        for (InstrumentationRule rule : classConfig.getActiveRules()) {
            if (log.isDebugEnabled()) {
                log.debug("Added hook to {} due to rule '{}'.", type.getName(), rule.getName());
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
     * @param classBeingRedefined the {@link  TypeDescriptionWithClassLoader}
     *
     * @return Then updated {@link  ClassInstrumentationConfiguration}
     */
    protected ClassInstrumentationConfiguration updateAndGetActiveConfiguration(Class<?> classBeingRedefined) {
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

    @EventListener(classes = {InspectitConfigChangedEvent.class}, condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorInstrumentedClassesCount() {
        selfMonitoring.recordMeasurement("instrumented-classes", instrumentedClasses.size());
    }
}
