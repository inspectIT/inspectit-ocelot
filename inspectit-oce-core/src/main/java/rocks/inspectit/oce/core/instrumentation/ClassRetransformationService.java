package rocks.inspectit.oce.core.instrumentation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.oce.core.service.BatchJobExecutorService;
import rocks.inspectit.oce.core.utils.CommonUtils;
import rocks.inspectit.oce.core.utils.StopWatch;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;

/**
 * This service is responsible for issuing classes to {@link Instrumentation#retransformClasses(Class[])} in batches.
 * Hereby, the service adds all Classes for which a retransformation is requested to a queue.
 * It asynchronously takes elements form this queue in batches.
 * Before a class is submitted for retransformation, it is again checked if the retransformation is actually required.
 */
@Service
@Slf4j
public class ClassRetransformationService {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private AsyncClassTransformer transformer;

    /**
     * The queue of classes to process.
     * For each element in the queue, this service checks if a retransformation is still required.
     * If this is the case, then a retransformation is triggered.
     */
    private Set<Class<?>> pendingClasses = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * The background job repeatedly executing {@link #retransformClasses(int)}
     */
    private BatchJobExecutorService.BatchJob retransformJob;

    /**
     * Calling this mehtod lets this service know that the instrumentation configuration for the given class has changed and that it requires a retransformation.
     *
     * @param clazz the clazz to put to the working queue
     */
    public void requestRetransformation(Class<?> clazz) {
        pendingClasses.add(clazz);
    }

    @PostConstruct
    private void init() {
        val settings = env.getCurrentConfig().getInstrumentation().getInternal();
        Duration delay = settings.getClassConfigurationCheckInterBatchDelay();
        int batchSize = settings.getClassRetransformBatchSize();
        retransformJob = executor.startJob(this::retransformClasses, batchSize, delay, delay);
    }

    /**
     * Reconfigures the jobs batch sizes and the inter batch delay on configuration changes.
     *
     * @param ev the change event
     */
    @EventListener
    private void configEventListener(InspectitConfigChangedEvent ev) {
        val settings = ev.getNewConfig().getInstrumentation().getInternal();
        retransformJob.setBatchSizes(settings.getClassRetransformBatchSize());
        retransformJob.setInterBatchDelay(settings.getClassRetransformInterBatchDelay());
    }

    /**
     * This method is regularly called through {@link #retransformJob} to process elements from {@link #pendingClasses}.
     * The goal is to get and remove batchSize classes from the queueand to invoke {@link Instrumentation#retransformClasses(Class[])} on them.
     * <p>
     * The package access is for testing.
     */
    void retransformClasses(int batchSize) {
        val watch = new StopWatch();
        if (!transformer.isShuttingDown()) {
            Set<Class<?>> classesToRetransform = new HashSet<>();
            try {
                Optional<Class<?>> nextClass = CommonUtils.pollElementFromSet(pendingClasses);
                while (classesToRetransform.size() < batchSize && nextClass.isPresent()) {
                    Class<?> clazz = nextClass.get();
                    pendingClasses.remove(clazz);

                    //the last up-date check might have been a long time ago... is the retransformation still required?
                    if (doesClassRequireRetransformation(clazz)) {
                        classesToRetransform.add(clazz);
                    }
                    nextClass = CommonUtils.pollElementFromSet(pendingClasses);
                }
            } catch (Exception e) {
                log.error("Error selecting Elements retransformation", e);
            }

            if (!classesToRetransform.isEmpty()) {
                try {
                    instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[]{}));
                    log.debug("Retransformed {} classes in {} ms, {} classes left to retransform", classesToRetransform.size(), watch.getElapsedMillis(), pendingClasses.size());
                } catch (Exception e) {
                    log.error("Error applying instrumentation:", e);
                }
            }
        }
    }

    /**
     * Todo: where to actually put this method... ? (Code duplication)
     *
     * @param clazz
     * @return
     */
    private boolean doesClassRequireRetransformation(Class<?> clazz) {
        val typeDescr = TypeDescription.ForLoadedType.of(clazz);
        InstrumentationSettings instrSettings = env.getCurrentConfig().getInstrumentation();
        val requestedConfig = ClassInstrumentationConfiguration.getFor(typeDescr, instrSettings, specialSensors);

        val activeConfig = transformer.getActiveClassInstrumentationConfiguration(clazz);

        return !activeConfig.isSameAs(typeDescr, requestedConfig);
    }

}
