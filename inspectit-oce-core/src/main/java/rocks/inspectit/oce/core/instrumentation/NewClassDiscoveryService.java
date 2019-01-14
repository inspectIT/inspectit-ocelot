package rocks.inspectit.oce.core.instrumentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitEnvironment;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
public class NewClassDiscoveryService {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private AsyncClassTransformer transformer;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private ScheduledExecutorService exec;


    private Set<Class<?>> knownClasses = Collections.newSetFromMap(new WeakHashMap<>());

    private Set<Consumer<Set<Class<?>>>> classDiscoveryListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Runnable updateCheckTask = () -> {
        long timePassed = System.currentTimeMillis() - transformer.getLastNewClassDefinitionTimestamp().get();
        if (timePassed < env.getCurrentConfig().getInstrumentation().getInternal().getMaxClassDefinitionDelay().toMillis()) {
            log.debug("Checking for new classes...");
            Set<Class<?>> newClasses = new HashSet<>();
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                if (!knownClasses.contains(clazz)) {
                    knownClasses.add(clazz);
                    newClasses.add(clazz);
                }
            }
            if (!newClasses.isEmpty()) {
                log.debug("{} new classes found (last class define {} ms ago)", newClasses.size(), timePassed);
                classDiscoveryListeners.forEach(lis -> lis.accept(newClasses));
            } else {
                log.debug("No new classes found (last class define {} ms ago)", timePassed);
            }
        }
        scheduleUpdateCheck();
    };


    public void addDiscoveryListener(Consumer<Set<Class<?>>> listener) {
        classDiscoveryListeners.add(listener);
    }

    @PostConstruct
    private void init() {
        knownClasses.addAll(Arrays.<Class<?>>asList(instrumentation.getAllLoadedClasses()));
        scheduleUpdateCheck();
    }

    private void scheduleUpdateCheck() {
        Duration delay = env.getCurrentConfig().getInstrumentation().getInternal().getNewClassDiscoveryInterval();
        exec.schedule(updateCheckTask, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

}
