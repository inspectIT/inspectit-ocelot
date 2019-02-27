package rocks.inspectit.oce.core.instrumentation;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.event.ClassInstrumentedEvent;
import rocks.inspectit.oce.core.instrumentation.event.IClassDiscoveryListener;
import rocks.inspectit.oce.core.instrumentation.event.TransformerShutdownEvent;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.oce.core.service.BatchJobExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for making sure that for every class the instrumentation
 * eventually is applied as specified in the active {@link rocks.inspectit.oce.core.config.model.InspectitConfig}
 * <p>
 * Whenever a classes instrumentation configuration might have changes, this class gets put into this services working queue.
 * This service is then responsible for filtering out the classes whose instrumentaiton actually has
 * changed and triggers the retransform for these.
 */
@Service
@Slf4j
public class InstrumentationManager implements IClassDiscoveryListener {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    /**
     * For each class we remember the applied instrumentation.
     * This allows us to check if a retransformation is required.
     */
    private Cache<Class<?>, ClassInstrumentationConfiguration> activeInstrumentations =
            CacheBuilder.newBuilder().weakKeys().build();

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
        TypeDescription type = event.getInstrumentedClassDescription();
        Class<?> clazz = event.getInstrumentedClass();

        if (ClassInstrumentationConfiguration.NO_INSTRUMENTATION.isSameAs(type, config)) {
            activeInstrumentations.invalidate(clazz);
        } else {
            activeInstrumentations.put(clazz, config);
        }

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
     * package-private for testing.
     *
     * @param batchSize the number of classes to take from {@link #pendingClasses} and to retransform per batch
     */
    void checkClassesForConfigurationUpdates(BatchSize batchSize) {
        List<Class<?>> classesToRetransform = getBatchOfClassesToRetransform(batchSize);

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
     * Package private for testing.
     *
     * @param batchSize the configured batch sizes
     * @return the classes which need retransformation
     */
    List<Class<?>> getBatchOfClassesToRetransform(BatchSize batchSize) {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("instrumentation-analysis")) {
            List<Class<?>> classesToRetransform = new ArrayList<>();
            val watch = Stopwatch.createStarted();
            try {

                int checkedClassesCount = 0;

                Iterator<Class<?>> queueIterator = pendingClasses.asMap().keySet().iterator();
                while (queueIterator.hasNext()) {

                    Class<?> clazz = queueIterator.next();
                    queueIterator.remove();
                    checkedClassesCount++;

                    if (doesClassRequireRetransformation(clazz)) {
                        classesToRetransform.add(clazz);
                    }

                    if (checkedClassesCount >= batchSize.maxClassesToCheck
                            || classesToRetransform.size() >= batchSize.maxClassesToRetransform) {
                        break;
                    }
                }
                if (checkedClassesCount > 0) {
                    log.debug("Checked configuration of {} classes in {} ms, {} classes left to check",
                            checkedClassesCount, watch.elapsed(TimeUnit.MILLISECONDS), pendingClasses.size());
                }
            } catch (Exception e) {
                log.error("Error checking for class instrumentation configuration updates", e);
            }
            return classesToRetransform;
        }
    }

    boolean doesClassRequireRetransformation(Class<?> clazz) {
        val typeDescr = TypeDescription.ForLoadedType.of(clazz);
        ClassInstrumentationConfiguration requestedConfig = configResolver.getClassInstrumentationConfiguration(clazz);
        val activeConfig = activeInstrumentations.getIfPresent(clazz);
        if (activeConfig == null) {
            return !requestedConfig.isNoInstrumentation();
        } else {
            return !activeConfig.isSameAs(typeDescr, requestedConfig);
        }
    }

    @EventListener(classes = {InspectitConfigChangedEvent.class},
            condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorQueueSize() {
        selfMonitoring.recordMeasurement("instrumentation-analysis-queue-size", pendingClasses.size());
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
