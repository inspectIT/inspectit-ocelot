package rocks.inspectit.oce.core.instrumentation;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.oce.core.instrumentation.event.IClassDiscoveryListener;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class NewClassDiscoveryService implements IClassDefinitionListener {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private ScheduledExecutorService exec;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    /**
     * package private for unit testing
     */
    @Autowired
    List<IClassDiscoveryListener> listeners;

    private Set<Class<?>> knownClasses = Collections.newSetFromMap(new WeakHashMap<>());

    private volatile boolean isShuttingDown = false;

    /**
     * Future for the scheduled {@link #updateCheckTask} to make it cancelable.
     */
    private Future<?> updateCheckFuture;

    /**
     * Stores the timestamp for the last time a class was defined.
     */
    private AtomicLong numTrialsSinceLastClassDefinition = new AtomicLong(0);

    private Runnable updateCheckTask = () -> {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("class-discovery")) {
            long maxTrials = env.getCurrentConfig().getInstrumentation().getInternal().getNumClassDiscoveryTrials();
            if (numTrialsSinceLastClassDefinition.incrementAndGet() <= maxTrials) {
                val watch = Stopwatch.createStarted();

                log.debug("Checking for new classes...");
                Set<Class<?>> newClasses = new HashSet<>();
                for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                    if (!knownClasses.contains(clazz)) {
                        knownClasses.add(clazz);
                        newClasses.add(clazz);
                    }
                }
                long elapsedMS = watch.elapsed(TimeUnit.MILLISECONDS);
                if (!newClasses.isEmpty()) {
                    log.debug("{} new classes found, check took {} ms", newClasses.size(), elapsedMS);
                    listeners.forEach(lis -> lis.onNewClassesDiscovered(newClasses));
                } else {
                    log.debug("No new classes found, check took {} ms", elapsedMS);
                }
            }
            if (!isShuttingDown) {
                scheduleUpdateCheck();
            }
        }
    };


    @Override
    public void onNewClassDefined(String className, ClassLoader loader) {
        numTrialsSinceLastClassDefinition.lazySet(0L);
    }

    /**
     * package private for unit testing
     */
    @PostConstruct
    void init() {
        Set<Class<?>> allClasses = new HashSet<>(Arrays.<Class<?>>asList(instrumentation.getAllLoadedClasses()));
        knownClasses.addAll(allClasses);
        listeners.forEach(lis -> lis.onNewClassesDiscovered(allClasses));
        scheduleUpdateCheck();
    }

    /**
     * package private for unit testing
     */
    @PreDestroy
    void destroy() {
        isShuttingDown = true;
        updateCheckFuture.cancel(false);
    }

    private void scheduleUpdateCheck() {
        Duration delay = env.getCurrentConfig().getInstrumentation().getInternal().getNewClassDiscoveryInterval();
        updateCheckFuture = exec.schedule(updateCheckTask, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

}
