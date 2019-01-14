package rocks.inspectit.oce.core.instrumentation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is responsible for managing and triggering instrumentation.
 * Any other class performing Instrumentation gets called by this class.
 */
@Service
@Slf4j
public class InstrumentationManager {

    /**
     * These classes are ignored when found on the bootstrap.
     * Those are basically the inspectIT classes and OpenCensus with its dependencies
     */
    private static final List<String> IGNORED_BOOTSTRAP_PACKAGES = Arrays.asList(
            "rocks.inspectit",
            "io.opencensus",
            "io.grpc",
            "com.lmax.disruptor",
            "com.google"
    );

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private List<SpecialSensor> specialSensors;

    @Autowired
    private SelfMonitoringService selfMonitoring;


    /**
     * This thread performs asynchronous updates to the instrumentation when the config changes at runtime.
     * When configuration changes occur while the thread is instrumenting, it will redo an instrumentation
     * when its current process is finished.
     */
    private Thread instrumentationThread;

    /**
     * Holds the settings which are currently being applied by the {@link #instrumentationThread} or have been applied last if it is waiting
     */
    InstrumentationSettings appliedInstrumentation = null;

    /**
     * The previously applied transformation, if any was applied.
     * This is used to revert previous changes when a new instrumentation is performed
     * or to perform a clean shutdown if the Agent is shutdown but the JVM is not.
     */
    Optional<ResettableClassFileTransformer> previousInstrumentation = Optional.empty();

    /**
     * This reference stores the instrumentation settings which the {@link #instrumentationThread} should apply next.
     * When this field differs from {@link #appliedInstrumentation} and {@link #instrumentationThreadMonitor} is notified,
     * the {@link #instrumentationThread} wakes up and performs the instrumentation.
     * If the {@link #instrumentationThread} is currently already applying an instrumentation, it will
     * apply the new settings as soon as applying the previous ones is finished.
     */
    private InstrumentationSettings instrumentationToApply;

    /**
     * A flag indicating that the {@link #instrumentationThread} should undo any previous instrumentations and terminate
     */
    private AtomicBoolean exitFlag = new AtomicBoolean(false);

    /**
     * The monitor used to notify {@link #instrumentationThread} that either {@link #exitFlag} or {@link #instrumentationToApply} has changed.
     */
    private Object instrumentationThreadMonitor = new Object();

    @PostConstruct
    private synchronized void init() {
        instrumentationToApply = env.getCurrentConfig().getInstrumentation();
        //Perform an initial synchronous transformation on startup
        applyInstrumentation(instrumentationToApply, false);
        startInstrumentationThread();
    }

    @PreDestroy
    private synchronized void destroy() {
        // we only need to wait for the deinstrumentation if the JVM is not shutting down,
        // otherwise we don't care about the state of the classes left behind
        if (!isJVMShuttingDown()) {
            exitFlag.set(true);
            synchronized (instrumentationThreadMonitor) {
                instrumentationThreadMonitor.notify();
            }
            try {
                instrumentationThread.join();
            } catch (InterruptedException e) {
                log.error("Error waiting for instrumentation thread to terminate", e);
            }
        }
    }

    @EventListener
    private synchronized void checkForConfigChanges(InspectitConfigChangedEvent ev) {
        instrumentationToApply = ev.getNewConfig().getInstrumentation();
        synchronized (instrumentationThreadMonitor) {
            instrumentationThreadMonitor.notify();
        }
    }


    private void startInstrumentationThread() {
        instrumentationThread = new Thread(() -> {
            while (!exitFlag.get()) {
                synchronized (instrumentationThreadMonitor) {
                    while (!exitFlag.get() && instrumentationToApply.equals(appliedInstrumentation)) {
                        try {
                            instrumentationThreadMonitor.wait();
                        } catch (InterruptedException exc) {
                            log.error("Exiting instrumentation thread due to interrupt");
                            exitFlag.set(true);
                        }
                    }
                }
                previousInstrumentation.ifPresent(instr -> {
                    log.info("Removing previous instrumentation...");
                    try (val s = selfMonitoring.withSelfMonitoring(getClass().getSimpleName() + "-deinstrumentation")) {
                        instr.reset(instrumentation,
                                AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                                AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(instrumentationToApply.getBatchSize()),
                                AgentBuilder.RedefinitionStrategy.Listener.Pausing.of(instrumentationToApply.getInterBatchPause().toMillis(), TimeUnit.MILLISECONDS));
                    }
                    log.info("Instrumentation removed.");
                });

                //only apply a new instrumentation if we are not exiting
                if (!exitFlag.get()) {
                    applyInstrumentation(instrumentationToApply, true);
                }
            }
        });
        instrumentationThread.setName("inspectit-instrumenter");
        instrumentationThread.setDaemon(true);
        instrumentationThread.start();
    }

    private void applyInstrumentation(InstrumentationSettings instrConf, boolean batching) {

        appliedInstrumentation = instrConf;
        try (val s = selfMonitoring.withSelfMonitoring(getClass().getSimpleName() + "-instrumentation")) {

            log.info("Performing instrumentation...");
            AgentBuilder baseAgent = new AgentBuilder.Default().disableClassFormatChanges();
            if (batching) {
                baseAgent = baseAgent.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(instrConf.getBatchSize()))
                        .with(AgentBuilder.RedefinitionStrategy.Listener.Pausing.of(instrConf.getInterBatchPause().toMillis(), TimeUnit.MILLISECONDS));
            } else {
                baseAgent = baseAgent.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
            }
            baseAgent = setupIgnoredClasses(baseAgent)
                    .with(new AgentBuilder.Listener.Adapter() {

                        @Override
                        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                            log.error("Error transforming {} from class loader {}", typeName, classLoader, throwable);
                        }

                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                            log.debug("Starting transformation of {}", typeDescription.getName());
                        }
                    });

            AgentBuilder agent = baseAgent;
            for (val sensor : specialSensors) {
                if (sensor.isEnabledForConfig(instrConf)) {
                    agent = sensor.instrument(instrConf, agent);
                }
            }
            previousInstrumentation = Optional.of(agent.installOn(instrumentation));
        }
        log.info("Instrumentation is done.");
    }

    /**
     * Unfortunately issuing multiple AgentBuilder#ignore calls only takes the last one.
     * Therefore we have to build a custom matcher.
     */
    private AgentBuilder setupIgnoredClasses(AgentBuilder baseAgent) {

        ClassLoader inspectITClassLoader = getClass().getClassLoader();

        return baseAgent.ignore((type, classloader, module, clazz, pd) -> {
            if (classloader == inspectITClassLoader) {
                return true;
            } else if (classloader == null) {
                return IGNORED_BOOTSTRAP_PACKAGES.stream()
                        .map(pkg -> pkg + ".")
                        .filter(prefix -> type.getName().startsWith(prefix))
                        .findAny().isPresent();
            }
            return false;
        });
    }

    private static boolean isJVMShuttingDown() {
        Thread dummyHook = new Thread(() -> {
        });
        try {
            Runtime.getRuntime().addShutdownHook(dummyHook);
            Runtime.getRuntime().removeShutdownHook(dummyHook);
        } catch (IllegalStateException e) {
            return true; //thrown by addShutdownHook when the JVM is already shutting down
        }
        return false;
    }


}
