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

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;

/**
 * This class is responsible for managing and triggering instrumentation.
 * Any other class performing Instrumentation gets called by this class.
 */
@Service
@Slf4j
public class InstrumentationManager {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private AsyncClassTransformer classTransformer;

    @Autowired
    private NewClassDiscoveryService classDisoveryService;

    @Autowired
    private Instrumentation instrumentation;


    private Set<Class<?>> classesToCheckForUpToDate = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private Set<Class<?>> classesToTriggerRetransform = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private BatchJobExecutorService.BatchJob configCheckJob;

    private BatchJobExecutorService.BatchJob retransformJob;


    @PostConstruct
    private void init() {
        classDisoveryService.addDiscoveryListener(classesToCheckForUpToDate::addAll);
        //this guarantees that eventually all classes will be correctly instrumented even in case of race conditions
        classTransformer.addPostInstrumentationListener((clazz, conf) -> classesToCheckForUpToDate.add(clazz));
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            classesToCheckForUpToDate.add(clazz);
        }
        startUpdateCheckService();
        startRetransformationService();
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent ev) {
        val oldInstrumentation = ev.getOldConfig().getInstrumentation();
        val newInstrumentation = ev.getNewConfig().getInstrumentation();
        if (!Objects.equals(oldInstrumentation, newInstrumentation)) {
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                classesToCheckForUpToDate.add(clazz);
            }
        }
        InternalSettings newInternal = newInstrumentation.getInternal();
        configCheckJob.setBatchSizes(newInternal.getClassConfigurationCheckBatchSize());
        configCheckJob.setInterBatchDelay(newInternal.getClassConfigurationCheckInterBatchDelay());
        retransformJob.setBatchSizes(newInternal.getClassRetransformBatchSize());
        retransformJob.setInterBatchDelay(newInternal.getClassRetransformInterBatchDelay());
    }


    private void startUpdateCheckService() {
        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        int batchSizes = conf.getClassConfigurationCheckBatchSize();
        Duration delay = conf.getClassConfigurationCheckInterBatchDelay();
        configCheckJob = executor.startJob((batchSize) -> {
            if (classTransformer.isShuttingDown()) {
                return true; //nothing to do anymore
            }
            try {
                int numChecks = 0;
                Optional<Class<?>> nextClass = CommonUtils.pollElementFromSet(classesToCheckForUpToDate);
                while (numChecks < batchSize && nextClass.isPresent()) {
                    numChecks++;
                    checkUpToDate(nextClass.get());
                    nextClass = CommonUtils.pollElementFromSet(classesToCheckForUpToDate);
                }
                if (numChecks > 0) {
                    log.debug("Checked configuration of {} classes, {} left to check", numChecks, classesToCheckForUpToDate.size());
                }
            } catch (Exception e) {
                log.error("Error checking for class instrumentation configuration updates", e);
            }
            return false; //we are never done
        }, batchSizes, delay, delay);
    }

    private synchronized void checkUpToDate(Class<?> clazz) {
        classesToCheckForUpToDate.remove(clazz);
        if (!classTransformer.isIgnoredClass(clazz)) {
            val typeDescr = TypeDescription.ForLoadedType.of(clazz);
            InstrumentationSettings instrSettings = env.getCurrentConfig().getInstrumentation();
            val requestedConfig = ClassInstrumentationConfiguration.getFor(typeDescr, instrSettings, specialSensors);

            val activeConfig = classTransformer.getActiveClassInstrumentationConfiguration(clazz);

            if (!activeConfig.isSameAs(typeDescr, requestedConfig)) {
                log.debug("Detected config change for {}, queing for retransform", clazz.getName());
                classesToTriggerRetransform.add(clazz);
            }
        }
    }

    private void startRetransformationService() {
        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        int batchSizes = conf.getClassRetransformBatchSize();
        Duration delay = conf.getClassRetransformInterBatchDelay();
        retransformJob = executor.startJob((batchSize) -> {
            if (classTransformer.isShuttingDown()) {
                return true; //nothing to do anymore
            }
            Set<Class<?>> classesToRetransform = new HashSet<>();
            try {
                Optional<Class<?>> nextClass = CommonUtils.pollElementFromSet(classesToTriggerRetransform);
                while (classesToRetransform.size() < batchSize && nextClass.isPresent()) {
                    Class<?> clazz = nextClass.get();
                    classesToTriggerRetransform.remove(clazz);
                    //the last up-date check might have been a long time ago... is the retransformation still required?
                    if (!classTransformer.isIgnoredClass(clazz)) {
                        val typeDescr = TypeDescription.ForLoadedType.of(clazz);
                        InstrumentationSettings instrSettings = env.getCurrentConfig().getInstrumentation();
                        val requestedConfig = ClassInstrumentationConfiguration.getFor(typeDescr, instrSettings, specialSensors);

                        val activeConfig = classTransformer.getActiveClassInstrumentationConfiguration(clazz);

                        if (!activeConfig.isSameAs(typeDescr, requestedConfig)) {
                            classesToRetransform.add(clazz);
                        } else {
                            //doiwngrade in case of race conditions
                            classesToCheckForUpToDate.add(clazz);
                        }
                    }
                    nextClass = CommonUtils.pollElementFromSet(classesToTriggerRetransform);
                }
            } catch (Exception e) {
                log.error("Error selecting Elements retransformation", e);
            }
            if (!classesToRetransform.isEmpty()) {
                try {
                    log.debug("Retransforming {} classes in a batch, {} left to retransform", classesToRetransform.size(), classesToTriggerRetransform.size());
                    instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[]{}));
                } catch (Exception e) {
                    log.error("Error applying instrumentation:", e);
                }
            }
            return false; //batch job is never done
        }, batchSizes, delay, delay);
    }

}
