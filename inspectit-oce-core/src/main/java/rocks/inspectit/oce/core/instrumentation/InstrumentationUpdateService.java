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
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
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
 * This class is responsible for making sure that for every class the instrumentation
 * eventually is applied as specified in the active {@link rocks.inspectit.oce.core.config.model.InspectitConfig}
 * <p>
 * Whenever a classes instrumentation configuration might have changes, this class gets put into this services working queue.
 * This service is then responsible for filtering out the classes whose instrumentaiton actually has
 * changed and submits them to the {@link ClassRetransformationService}.
 */
@Service
@Slf4j
public class InstrumentationUpdateService {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private AsyncClassTransformer transformer;

    @Autowired
    private NewClassDiscoveryService classDisoveryService;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private ClassRetransformationService retransformationService;


    private Set<Class<?>> pendingClasses = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private BatchJobExecutorService.BatchJob classCheckerJob;


    @PostConstruct
    private void init() {
        // all newly loaded classes have to be checked if they need instrumentation
        classDisoveryService.addDiscoveryListener(pendingClasses::addAll);
        // it is possible that we think a class is up to date while it is actually being instrumented with an
        // outdated instrumentation configuration .for this reason we recheck every class after it has been instrumented.
        transformer.addPostInstrumentationListener((clazz, conf) -> pendingClasses.add(clazz));

        //all already loaded classes need to be checked if they need instrumentation
        requestConfigUpdateForAllLoadedClasses();

        startClassCheckerJob();

    }

    private void startClassCheckerJob() {
        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        int batchSizes = conf.getClassConfigurationCheckBatchSize();
        Duration delay = conf.getClassConfigurationCheckInterBatchDelay();
        classCheckerJob = executor.startJob(this::checkClassesForConfigurationUpdates, batchSizes, delay, delay);
    }

    /**
     * Forces all already loaded classes to be checked if their instrumentation is up to date.
     */
    private void requestConfigUpdateForAllLoadedClasses() {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            pendingClasses.add(clazz);
        }
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent ev) {
        val oldInstrumentation = ev.getOldConfig().getInstrumentation();
        val newInstrumentation = ev.getNewConfig().getInstrumentation();

        //if the instrumentation configuration has changed all classes need to be reevalauted
        if (!Objects.equals(oldInstrumentation, newInstrumentation)) {
            requestConfigUpdateForAllLoadedClasses();
        }

        InternalSettings newInternal = newInstrumentation.getInternal();
        classCheckerJob.setBatchSizes(newInternal.getClassConfigurationCheckBatchSize());
        classCheckerJob.setInterBatchDelay(newInternal.getClassConfigurationCheckInterBatchDelay());
    }


    /**
     * Takes numberOfClassesToProcess classes from {@link #pendingClasses} and calls {@link #ensureInstrumentationIsUpToDate(Class)}
     * for them. This method is invoked regularly by {@link #classCheckerJob}.
     * <p>
     * This method is package-private for testing.
     *
     * @param numberOfClassesToProcess the number of classes to take from {@link #pendingClasses}
     */
    void checkClassesForConfigurationUpdates(int numberOfClassesToProcess) {
        if (transformer.isShuttingDown()) {
            return; //nothing to do anymore
        }

        val watch = new StopWatch();
        try {
            int processedClassesCount;
            for (processedClassesCount = 0; processedClassesCount < numberOfClassesToProcess; processedClassesCount++) {
                Optional<Class<?>> nextClass = CommonUtils.pollElementFromSet(pendingClasses);
                if (nextClass.isPresent()) {
                    pendingClasses.remove(nextClass.get());
                    ensureInstrumentationIsUpToDate(nextClass.get());
                } else {
                    break;
                }
            }
            if (processedClassesCount > 0) {
                log.debug("Checked configuration of {} classes in {} ms, {} classes left to check", processedClassesCount, watch.getElapsedMillis(), pendingClasses.size());
            }
        } catch (Exception e) {
            log.error("Error checking for class instrumentation configuration updates", e);
        }
    }

    /**
     * Checks if the active instrumentation for the given class matches the one requested through the configuration.
     * If the class is not up to date it issued to the {@link #retransformationService}
     *
     * @param clazz the class to check.
     */
    private void ensureInstrumentationIsUpToDate(Class<?> clazz) {
        if (!transformer.isIgnoredClass(clazz) && doesClassRequireRetransformation(clazz)) {
            log.debug("Detected config change for {}, queing for retransform", clazz.getName());
            retransformationService.requestRetransformation(clazz);
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
