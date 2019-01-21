package rocks.inspectit.oce.core.instrumentation;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.utils.StopWatch;

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
    private List<IClassDiscoveryListener> listeners;

    private Set<Class<?>> knownClasses = Collections.newSetFromMap(new WeakHashMap<>());

    private volatile boolean isShuttingDown = false;

    /**
     * Future for the scheduled {@link #updateCheckTask} to make it cancelable.
     */
    private Future<?> updateCheckFuture;

    /**
     * Stores the timestamp for the last time a class was defined.
     */
    private AtomicLong lastNewClassDefinitionTimestamp = new AtomicLong();

    private Runnable updateCheckTask = () -> {
        long timeSinceLastClassDefinition = System.currentTimeMillis() - lastNewClassDefinitionTimestamp.get();
        long maxDelay = env.getCurrentConfig().getInstrumentation().getInternal().getMaxClassDefinitionDelay().toMillis();
        long minDelay = env.getCurrentConfig().getInstrumentation().getInternal().getMinClassDefinitionDelay().toMillis();
        if (timeSinceLastClassDefinition >= minDelay && timeSinceLastClassDefinition <= maxDelay) {
            val watch = new StopWatch();

            log.debug("Checking for new classes...");
            Set<Class<?>> newClasses = new HashSet<>();
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                if (!knownClasses.contains(clazz)) {
                    knownClasses.add(clazz);
                    newClasses.add(clazz);
                }
            }
            if (!newClasses.isEmpty()) {
                log.debug("{} new classes found, check took {} ms (last class define {} ms ago)", newClasses.size(), watch.getElapsedMillis(), timeSinceLastClassDefinition);
                listeners.forEach(lis -> lis.newClassesDiscovered(newClasses));
            } else {
                log.debug("No new classes found, check took {} ms (last class define {} ms ago)", watch.getElapsedMillis(), timeSinceLastClassDefinition);
            }
        }
        if (!isShuttingDown) {
            scheduleUpdateCheck();
        }
    };


    @Override
    public void newClassDefined(String className, ClassLoader loader) {
        lastNewClassDefinitionTimestamp.set(System.currentTimeMillis());
    }

    @PostConstruct
    private void init() {
        Set<Class<?>> allClasses = new HashSet<>(Arrays.<Class<?>>asList(instrumentation.getAllLoadedClasses()));
        knownClasses.addAll(allClasses);
        listeners.forEach(lis -> lis.newClassesDiscovered(allClasses));
        scheduleUpdateCheck();
    }

    @PreDestroy
    private void destroy() {
        isShuttingDown = true;
        updateCheckFuture.cancel(false);
    }

    private void scheduleUpdateCheck() {
        Duration delay = env.getCurrentConfig().getInstrumentation().getInternal().getNewClassDiscoveryInterval();
        updateCheckFuture = exec.schedule(updateCheckTask, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

}
