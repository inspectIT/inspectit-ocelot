package rocks.inspectit.oce.core.instrumentation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.opencensus.stats.Aggregation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationScope;
import rocks.inspectit.oce.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.oce.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.oce.core.instrumentation.event.TransformerShutdownEvent;
import rocks.inspectit.oce.core.instrumentation.hook.DispatchHookAdvice;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.oce.core.utils.CommonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    List<IClassDefinitionListener> classDefinitionListeners;

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
     */
    private Cache<Class<?>, Boolean> instrumentedClasses = CacheBuilder.newBuilder().weakKeys().build();


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytecode) throws IllegalClassFormatException {
        if (classBeingRedefined == null) { // class is not loaded yet! we only redefine only loaded classes to prevent blocking
            classDefinitionListeners.forEach(lis -> lis.onNewClassDefined(className, loader));
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
        // this look guarantees through updateAndGetActiveConfiguration that no instrumentation is added after the lock is released
        synchronized (shutDownLock) {
            shuttingDown = true;
        }
        ctx.publishEvent(new TransformerShutdownEvent(this));
        if (!CommonUtils.isJVMShuttingDown()) {
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
        //elements are removed from instrumentedClasses by the updateAndGetActiveConfiguration method
        while (instrumentedClasses.size() > 0) {
            List<Class<?>> batchClasses = new ArrayList<>();
            int maxBatchSize = env.getCurrentConfig().getInstrumentation().getInternal().getClassRetransformBatchSize();

            Iterator<Class<?>> it = instrumentedClasses.asMap().keySet().iterator();
            for (int i = 0; i < maxBatchSize && it.hasNext(); i++) {
                batchClasses.add(it.next());
            }
            try {
                instrumentation.retransformClasses(batchClasses.toArray(new Class[]{}));
                Thread.yield(); //yield to allow the target application to do stuff between the batches
            } catch (UnmodifiableClassException e) {
                log.error("Error removing applied transformations", e);
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
                    log.debug("Redefining class: {}", classBeingRedefined.getName());
                }
                //Make a ByteBuddy builder based on the input bytecode
                ClassFileLocator byteCodeClassFileLocator = ClassFileLocator.Simple.of(classBeingRedefined.getName(), originalByteCode);
                DynamicType.Builder<?> builder = new ByteBuddy().redefine(classBeingRedefined, byteCodeClassFileLocator);

                //Apply the actual instrumentation onto the builders
                for (SpecialSensor specialSensor : classConf.getActiveSpecialSensors()) {
                    builder = specialSensor.instrument(classBeingRedefined, type, classConf.getActiveConfiguration(), builder);
                }

                // Apply the hook if necessary
                for (InstrumentationRule rule : classConf.getActiveRules()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Added hook to {} due to rule '{}'.", classBeingRedefined, rule.getName());
                    }
                    for (InstrumentationScope scope : rule.getScopes()) {
                        if (log.isTraceEnabled()) {
                            log.trace("|> {}", scope.getTypeMatcher());
                        }
                        builder = builder.visit(Advice.to(DispatchHookAdvice.class).on(scope.getMethodMatcher()));
                    }
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
                log.debug("Removing instrumentation of {}", classBeingRedefined);
                instrumentedClasses.invalidate(classBeingRedefined);
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
        if (selfMonitoring.isSelfMonitoringEnabled()) {
            val measure = selfMonitoring.getSelfMonitoringMeasureLong(
                    "instrumented-classes",
                    "The number of classes currently instrumented by inspectIT",
                    "classes",
                    Aggregation.LastValue::create);
            selfMonitoring.recordMeasurement(measure, instrumentedClasses.size());
        }
    }

}
