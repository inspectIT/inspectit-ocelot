package rocks.inspectit.oce.core.instrumentation;

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
import rocks.inspectit.oce.core.instrumentation.config.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.service.BatchJobExecutorService;
import rocks.inspectit.oce.core.utils.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;

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
    private NewClassDiscoveryService classDisoveryService;

    /**
     * Required to detect if the transformer is shutting down.
     * In this case no further retransforms will be triggered.
     */
    @Autowired
    private AsyncClassTransformer transformer;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    /**
     * For each class we remember the applied instrumentation.
     * This allows us to check if a retransform is required.
     */
    private Map<Class<?>, ClassInstrumentationConfiguration> activeInstrumentations =
            CacheBuilder.newBuilder().weakKeys().<Class<?>, ClassInstrumentationConfiguration>build().asMap();

    /**
     * A set containing all loaded classes.
     * This required because in case of a config change we need to reevalaute the intrumentation for each class.
     */
    private Set<Class<?>> loadedClasses = Collections.newSetFromMap(
            CacheBuilder.newBuilder().weakKeys().<Class<?>, Boolean>build().asMap());

    /**
     * The set of classes which might need instrumentation updates.
     * This service works through this set in batches.
     */
    private Set<Class<?>> pendingClasses = Collections.newSetFromMap(
            CacheBuilder.newBuilder().weakKeys().<Class<?>, Boolean>build().asMap());

    private BatchJobExecutorService.BatchJob<BatchSize> classInstrumentationJob;

    @Value
    private static class BatchSize {
        private int maxClassesToCheck;
        private int maxClassesToRetransform;
    }


    @PostConstruct
    private void init() {
        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        val batchSizes = new BatchSize(conf.getClassConfigurationCheckBatchSize(), conf.getClassRetransformBatchSize());
        Duration delay = conf.getInterBatchDelay();

        classInstrumentationJob = executor.startJob(this::checkClassesForConfigurationUpdates, batchSizes, delay, delay);
    }

    @PreDestroy
    private void destroy() {
        classInstrumentationJob.cancel();
    }

    @Override
    public void newClassesDiscovered(Set<Class<?>> newClasses) {
        loadedClasses.addAll(newClasses);
        pendingClasses.addAll(newClasses);
    }

    @EventListener
    private void classInstrumented(ClassInstrumentedEvent event) {
        ClassInstrumentationConfiguration config = event.getAppliedConfiguration();
        TypeDescription type = event.getInstrumentedClassDescription();
        Class<?> clazz = event.getInstrumentedClass();

        if (ClassInstrumentationConfiguration.NO_INSTRUMENTATION.isSameAs(type, config)) {
            activeInstrumentations.remove(clazz);
        } else {
            activeInstrumentations.put(clazz, config);
        }

        //there might be race conditions when a class is being retransformed
        //and we considered it up-to-date at the same time
        //for this reason we have to recheck every class after it has been instrumented
        pendingClasses.add(clazz);
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
        pendingClasses.addAll(loadedClasses);
    }


    /**
     * Processes a given amount of classes from {@link #pendingClasses}.
     * For the classes where it is required a retransform is triggered.
     * <p>
     * This method is package-private for testing.
     *
     * @param batchSize the number of classes to take from {@link #pendingClasses} and to retransform per batch
     */
    void checkClassesForConfigurationUpdates(BatchSize batchSize) {
        if (transformer.isShuttingDown()) {
            return; //nothing to do anymore
        }

        List<Class<?>> classesToRetransform = getBatchOfClassesToRetransform(batchSize);

        val watch = new StopWatch();
        if (!classesToRetransform.isEmpty()) {
            try {
                instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[]{}));
                log.debug("Retransformed {} classes in {} ms", classesToRetransform.size(), watch.getElapsedMillis());
            } catch (Exception e) {
                log.error("Error retransforming classes!", e);
            }
        }

    }

    private List<Class<?>> getBatchOfClassesToRetransform(BatchSize batchSize) {
        List<Class<?>> classesToRetransform = new ArrayList<>();

        StopWatch watch = new StopWatch();
        try {

            int checkedClassesCount = 0;

            Iterator<Class<?>> queueIterator = pendingClasses.iterator();
            while (queueIterator.hasNext()) {

                Class<?> clazz = queueIterator.next();
                queueIterator.remove();
                checkedClassesCount++;

                if (doesClassRequireRetransformation(clazz)) {
                    classesToRetransform.add(clazz);
                }

                if (checkedClassesCount == batchSize.maxClassesToCheck
                        || classesToRetransform.size() == batchSize.maxClassesToRetransform) {
                    break;
                }
            }
            if (checkedClassesCount > 0) {
                log.debug("Checked configuration of {} classes in {} ms, {} classes left to check", checkedClassesCount, watch.getElapsedMillis(), pendingClasses.size());
            }
        } catch (Exception e) {
            log.error("Error checking for class instrumentation configuration updates", e);
        }
        return classesToRetransform;
    }

    private boolean doesClassRequireRetransformation(Class<?> clazz) {
        val typeDescr = TypeDescription.ForLoadedType.of(clazz);
        ClassInstrumentationConfiguration requestedConfig = configResolver.getClassInstrumentationConfiguration(clazz);
        val activeConfig = activeInstrumentations.getOrDefault(clazz, ClassInstrumentationConfiguration.NO_INSTRUMENTATION);
        return !activeConfig.isSameAs(typeDescr, requestedConfig);
    }
}
