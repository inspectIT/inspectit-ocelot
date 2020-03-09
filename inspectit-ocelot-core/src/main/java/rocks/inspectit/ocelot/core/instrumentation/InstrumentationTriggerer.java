package rocks.inspectit.ocelot.core.instrumentation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InternalSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.ocelot.core.instrumentation.event.IClassDiscoveryListener;
import rocks.inspectit.ocelot.core.instrumentation.event.TransformerShutdownEvent;
import rocks.inspectit.ocelot.core.instrumentation.hook.HookManager;
import rocks.inspectit.ocelot.core.instrumentation.special.ClassLoaderDelegation;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.service.BatchJobExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for making sure that for every class the instrumentation and hooking
 * eventually is applied as specified in the active {@link InspectitConfig}
 * <p>
 * Whenever a classes instrumentation configuration might have changes, this class gets put into this services working queue.
 * This service is then responsible for filtering out the classes whose instrumentation actually has
 * changed and triggers the retransform for these.
 */
@Service
@Slf4j
public class InstrumentationTriggerer implements IClassDiscoveryListener {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private ClassLoaderDelegation classLoaderDelegation;

    @Autowired
    private InstrumentationManager instrumentationManager;

    @Autowired
    private HookManager hookManager;

    /**
     * The update of hooks which is currently in progress, null if none is in progress.
     */
    private HookManager.HookUpdate currentHookUpdate;

    @Autowired
    InstrumentationConfigurationResolver configResolver;

    /**
     * The set of classes which might need instrumentation updates.
     * This service works through this set in batches.
     * Package-private for testing.
     */
    Cache<Class<?>, Boolean> pendingClasses =
            CacheBuilder.newBuilder().weakKeys().build();

    private BatchJobExecutorService.BatchJob<BatchSize> classInstrumentationJob;


    @PostConstruct
    private void init() {
        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        val batchSizes = new BatchSize(conf.getClassConfigurationCheckBatchSize(), conf.getClassRetransformBatchSize());
        Duration delay = conf.getInterBatchDelay();

        classInstrumentationJob = executor.startJob(this::checkClassesForConfigurationUpdates, batchSizes, delay, delay);
    }

    @EventListener(TransformerShutdownEvent.class)
    @PreDestroy
    private void destroy() {
        classInstrumentationJob.cancel();
    }

    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        for (Class<?> clazz : newClasses) {
            pendingClasses.put(clazz, Boolean.TRUE);
        }
        selfMonitorQueueSize();
    }

    @EventListener
    private void classInstrumented(ClassInstrumentedEvent event) {
        ClassInstrumentationConfiguration config = event.getAppliedConfiguration();
        Class<?> clazz = event.getInstrumentedClass();

        //there might be race conditions when a class is being retransformed
        //and we considered it up-to-date at the same time
        //for this reason we have to recheck every class after it has been instrumented
        pendingClasses.put(clazz, Boolean.TRUE);
        selfMonitorQueueSize();
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent ev) {

        InternalSettings newInternal = ev.getNewConfig().getInstrumentation().getInternal();
        val batchSizes = new BatchSize(newInternal.getClassConfigurationCheckBatchSize(), newInternal.getClassRetransformBatchSize());
        classInstrumentationJob.setBatchSizes(batchSizes);
        classInstrumentationJob.setInterBatchDelay(newInternal.getInterBatchDelay());
    }

    @EventListener
    private void instrumentationConfigEventListener(InstrumentationConfigurationChangedEvent ev) {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            pendingClasses.put(clazz, Boolean.TRUE);
        }
        selfMonitorQueueSize();
    }

    /**
     * Processes a given amount of classes from {@link #pendingClasses}.
     * For the classes where it is required a retransform is triggered.
     * In addition for each class the hooks are updated.
     * package-private for testing.
     *
     * @param batchSize the number of classes to take from {@link #pendingClasses} and to retransform per batch
     */
    void checkClassesForConfigurationUpdates(BatchSize batchSize) {
        List<Class<?>> classesToRetransform = new ArrayList<>(getBatchOfClassesToRetransform(batchSize));

        try (val sm = selfMonitoring.withDurationSelfMonitoring("instrumentation-retransformation")) {
            val watch = Stopwatch.createStarted();
            if (!classesToRetransform.isEmpty()) {
                try {
                    instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[]{}));
                    log.debug("Retransformed {} classes in {} ms", classesToRetransform.size(), watch.elapsed(TimeUnit.MILLISECONDS));
                } catch (Throwable e) {
                    if (classesToRetransform.size() == 1) {
                        log.error("Error retransforming class '{}'", classesToRetransform.get(0).getName(), e);
                    } else {
                        log.error("Error retransforming batch of classes, retrying classes one by one.", e);
                        for (Class<?> clazz : classesToRetransform) {
                            try {
                                instrumentation.retransformClasses(clazz);
                            } catch (Throwable e2) {
                                log.error("Error retransforming class '{}'", clazz.getName(), e2);
                            }
                        }
                    }
                }
            }
        }
        selfMonitorQueueSize();
    }

    /**
     * Takes the configured amounts from {@link #pendingClasses} and checks if they need a retransformation.
     * In addition for each class the hooks are updated
     * Package private for testing.
     *
     * @param batchSize the configured batch sizes
     * @return the classes which need retransformation
     */
    @VisibleForTesting
    Set<Class<?>> getBatchOfClassesToRetransform(BatchSize batchSize) {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("instrumentation-analysis")) {
            Set<Class<?>> classesToRetransform = new HashSet<>();
            val watch = Stopwatch.createStarted();
            try {

                int checkedClassesCount = 0;

                Iterator<Class<?>> queueIterator = pendingClasses.asMap().keySet().iterator();
                while (queueIterator.hasNext()) {

                    Class<?> clazz = queueIterator.next();
                    queueIterator.remove();
                    checkedClassesCount++;

                    updateClass(clazz, classesToRetransform);

                    if (checkedClassesCount >= batchSize.maxClassesToCheck
                            || classesToRetransform.size() >= batchSize.maxClassesToRetransform) {
                        break;
                    }
                }
                if (checkedClassesCount > 0) {
                    log.debug("Checked configuration of {} classes in {} ms, {} classes left to check",
                            checkedClassesCount, watch.elapsed(TimeUnit.MILLISECONDS), pendingClasses.size());
                }
                if (pendingClasses.size() == 0 && currentHookUpdate != null) {
                    currentHookUpdate.commitUpdate();
                    log.info("Instrumentation has been updated!");
                    currentHookUpdate = null;
                }
            } catch (Exception e) {
                log.error("Error checking for class instrumentation configuration updates", e);
            }
            return classesToRetransform;
        }
    }

    /**
     * Checks the given class for updates.
     * This method first makes sure that our bootstrap classes are accessible by the given class.
     * If this is the case, then the {@link InstrumentationManager} and {@link HookManager}
     * are used to update the instrumentation and the hooks of the class.
     *
     * @param clazz                the class whose instrumentation should be checked
     * @param classesToRetransform if the class does require a change of the bytecode, it will be added to this set.
     */
    private void updateClass(Class<?> clazz, Set<Class<?>> classesToRetransform) {
        if (instrumentationManager.doesClassRequireRetransformation(clazz)) {
            applyClassLoaderDelegation(clazz, classesToRetransform);
            classesToRetransform.add(clazz);
        }
        try {
            //this is guaranteed to be invoked after applyClassLoaderDelegation if any hooking occurs
            //this is due to the fact doesClassRequireRetransformation return true when the first hook is added
            if (currentHookUpdate == null) {
                currentHookUpdate = hookManager.startUpdate();
            }
            currentHookUpdate.updateHooksForClass(clazz);
        } catch (Throwable t) {
            log.error("Error adding hooks to clazz {}", clazz.getName(), t);
        }
    }

    private void applyClassLoaderDelegation(Class<?> clazz, Set<Class<?>> classesToRetransform) {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("classloader-delegation")) {
            LinkedHashSet<Class<?>> classLoadersToRetransform = classLoaderDelegation.getClassLoaderClassesRequiringRetransformation(clazz.getClassLoader(), configResolver.getCurrentConfig());
            //the order is important here!
            for (Class<?> classLoaderToRetransform : classLoadersToRetransform) {
                classesToRetransform.remove(classLoaderToRetransform);
                if (instrumentationManager.doesClassRequireRetransformation(classLoaderToRetransform)) {
                    try {
                        log.debug("Retransforming {} due to classloader delegation", classLoaderToRetransform.getName());
                        instrumentation.retransformClasses(classLoaderToRetransform);
                    } catch (Throwable t) {
                        log.error("Error retransforming {} due to classloader delegation", classLoaderToRetransform.getName(), t);
                    }
                }
            }
        }
    }

    @EventListener(classes = {InspectitConfigChangedEvent.class},
            condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorQueueSize() {
        selfMonitoring.recordMeasurement("instrumentation-queue-size", pendingClasses.size());
    }

    /**
     * package private for testing.
     */
    @Value
    static class BatchSize {
        private int maxClassesToCheck;
        private int maxClassesToRetransform;
    }
}
