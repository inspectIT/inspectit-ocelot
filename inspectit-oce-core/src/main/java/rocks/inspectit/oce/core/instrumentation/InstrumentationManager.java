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
import rocks.inspectit.oce.core.utils.CommonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
            "io.opencensus.",
            "rocks.inspectit.",
            "io.grpc.",
            "com.lmax.disruptor.",
            "com.google."
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
    private InstrumentationThread instrumentationThread;

    /**
     * Holds the settings which are currently being applied by the {@link #instrumentationThread} or have been applied last if it is waiting
     */
    private InstrumentationSettings currentInstrumentationSettings = null;

    /**
     * This reference stores the instrumentation settings which the {@link #instrumentationThread} should apply next.
     * When this field differs from {@link #activeInstrumentation} and {@link #instrumentationThread} is notified,
     * the {@link #instrumentationThread} wakes up and performs the instrumentation.
     * If the {@link #instrumentationThread} is currently already applying an instrumentation, it will
     * apply the new settings as soon as applying the previous ones is finished.
     */
    private InstrumentationSettings nextInstrumentationSettings;

    /**
     * The previously applied transformation, if any was applied.
     * This is used to revert previous changes when a new instrumentation is performed
     * or to perform a clean shutdown if the Agent is shutdown but the JVM is not.
     */
    private Optional<ResettableClassFileTransformer> activeInstrumentation = Optional.empty();

    private final AgentBuilder.RawMatcher classExcludeMatcher = (typeDescription, classLoader, module, classBeingRedefined, protectionDomain) -> {
        if (classLoader == InstrumentationManager.class.getClassLoader()) {
            return true;
        } else if (classLoader == null) {
            return IGNORED_BOOTSTRAP_PACKAGES
                    .stream()
                    .anyMatch(packagePrefix -> typeDescription.getName().startsWith(packagePrefix));
        }
        return false;
    };

    @PostConstruct
    private synchronized void init() {
        nextInstrumentationSettings = env.getCurrentConfig().getInstrumentation();

        // Perform an initial synchronous transformation on startup
        applyInstrumentation(false);

        instrumentationThread = new InstrumentationThread(this);
        instrumentationThread.start();
    }

    @PreDestroy
    private synchronized void destroy() {
        // we only need to wait for the deinstrumentation if the JVM is not shutting down,
        // otherwise we don't care about the state of the classes left behind
        if (!CommonUtils.isJVMShuttingDown()) {
            instrumentationThread.setExitFlag(true);
            synchronized (instrumentationThread) {
                instrumentationThread.notify();
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
        nextInstrumentationSettings = ev.getNewConfig().getInstrumentation();
        synchronized (instrumentationThread) {
            instrumentationThread.notify();
        }
    }

    public boolean isNewInstrumentationAvailable() {
        return !nextInstrumentationSettings.equals(currentInstrumentationSettings);
    }

    public void applyInstrumentation(boolean batching) {
        try (val s = selfMonitoring.withSelfMonitoring(getClass().getSimpleName() + "-instrumentation")) {

            log.info("Performing instrumentation...");
            AgentBuilder baseAgent = new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .ignore(classExcludeMatcher)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);

            if (batching) {
                baseAgent = ((AgentBuilder.RedefinitionListenable.WithoutBatchStrategy) baseAgent)
                        .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(nextInstrumentationSettings.getBatchSize()))
                        .with(AgentBuilder.RedefinitionStrategy.Listener.Pausing.of(nextInstrumentationSettings.getInterBatchPause().toMillis(), TimeUnit.MILLISECONDS));
            }

            baseAgent.with(new AgentBuilder.Listener.Adapter() {
                @Override
                public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                    log.error("Error transforming {} from class loader {}", typeName, classLoader, throwable);
                }

                @Override
                public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                    log.debug("Starting transformation of {}", typeDescription.getName());
                }
            });

            for (val sensor : specialSensors) {
                if (sensor.isEnabledForConfig(nextInstrumentationSettings)) {
                    baseAgent = sensor.instrument(nextInstrumentationSettings, baseAgent);
                }
            }
            activeInstrumentation = Optional.of(baseAgent.installOn(instrumentation));
            currentInstrumentationSettings = nextInstrumentationSettings;
        }
        log.info("Instrumentation is done.");
    }

    public void resetInstrumentation() {
        activeInstrumentation.ifPresent(instr -> {
            log.info("Removing previous instrumentation...");
            try (val s = selfMonitoring.withSelfMonitoring(getClass().getSimpleName() + "-uninstrumentation")) {
                instr.reset(instrumentation,
                        AgentBuilder.RedefinitionStrategy.RETRANSFORMATION,
                        AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(nextInstrumentationSettings.getBatchSize()),
                        AgentBuilder.RedefinitionStrategy.Listener.Pausing.of(nextInstrumentationSettings.getInterBatchPause().toMillis(), TimeUnit.MILLISECONDS));
            }
            log.info("Instrumentation removed.");
        });
    }
}
