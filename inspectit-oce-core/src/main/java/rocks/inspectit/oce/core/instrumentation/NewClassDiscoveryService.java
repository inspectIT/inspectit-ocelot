package rocks.inspectit.oce.core.instrumentation;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.instrumentation.event.IClassDefinitionListener;
import rocks.inspectit.oce.core.instrumentation.event.IClassDiscoveryListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NewClassDiscoveryService implements IClassDefinitionListener {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private ScheduledExecutorService exec;

    /**
     * package private for unit testing
     */
    @Autowired
    List<IClassDiscoveryListener> listeners;

    /**
     * The function to get a timestamp in milliseconds.
     * package private for unit testing
     */
    Supplier<Long> timestampMS = System::currentTimeMillis;


    private Set<Class<?>> knownClasses = Collections.newSetFromMap(new WeakHashMap<>());

    private volatile boolean isShuttingDown = false;

    /**
     * Future for the scheduled {@link #updateCheckTask} to make it cancelable.
     */
    private Future<?> updateCheckFuture;

    /**
     * Stores the timestamp for the last time a class was defined.
     */
    private volatile long lastNewClassDefinitionTimestamp = 0;

    private Runnable updateCheckTask = () -> {
        long timeSinceLastClassDefinition = timestampMS.get() - lastNewClassDefinitionTimestamp;
        long maxDelay = env.getCurrentConfig().getInstrumentation().getInternal().getMaxClassDefinitionDelay().toMillis();
        long minDelay = env.getCurrentConfig().getInstrumentation().getInternal().getMinClassDefinitionDelay().toMillis();
        if (timeSinceLastClassDefinition >= minDelay && timeSinceLastClassDefinition <= maxDelay) {
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
                log.debug("{} new classes found, check took {} ms (last class define {} ms ago)",
                        newClasses.size(), elapsedMS, timeSinceLastClassDefinition);
                listeners.forEach(lis -> lis.onNewClassesDiscovered(newClasses));
            } else {
                log.debug("No new classes found, check took {} ms (last class define {} ms ago)",
                        elapsedMS, timeSinceLastClassDefinition);
            }
        }
        if (!isShuttingDown) {
            scheduleUpdateCheck();
        }
    };


    @Override
    public void onNewClassDefined(String className, ClassLoader loader) {
        lastNewClassDefinitionTimestamp = timestampMS.get();
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
