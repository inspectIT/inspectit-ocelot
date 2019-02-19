package rocks.inspectit.oce.core.instrumentation.hook;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.opencensus.stats.Aggregation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.bootstrap.Instances;
import rocks.inspectit.oce.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.oce.bootstrap.instrumentation.noop.NoopMethodHook;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InternalSettings;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.oce.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.event.IClassDiscoveryListener;
import rocks.inspectit.oce.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.oce.core.service.BatchJobExecutorService;
import rocks.inspectit.oce.core.utils.CommonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HookManager implements IClassDiscoveryListener {

    @Autowired
    private BatchJobExecutorService executor;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private MethodHookGenerator hookGenerator;

    private LoadingCache<Class<?>, ConcurrentHashMap<String, MethodHook>> hooks = CacheBuilder.newBuilder().weakKeys().build(
            new CacheLoader<Class<?>, ConcurrentHashMap<String, MethodHook>>() {
                @Override
                public ConcurrentHashMap<String, MethodHook> load(Class<?> key) throws Exception {
                    return new ConcurrentHashMap<>();
                }
            });


    /**
     * The set of classes which might need hook updates.
     * This service works through this set in batches.
     */
    @VisibleForTesting
    Cache<Class<?>, Boolean> pendingClasses =
            CacheBuilder.newBuilder().weakKeys().build();


    private BatchJobExecutorService.BatchJob<Integer> classInstrumentationJob;

    @PostConstruct
    void init() {
        Instances.hookManager = this::getHook;

        InternalSettings conf = env.getCurrentConfig().getInstrumentation().getInternal();
        Duration delay = conf.getInterBatchDelay();

        classInstrumentationJob = executor.startJob(this::checkClassesForConfigurationUpdates, conf.getClassConfigurationCheckBatchSize(), delay, delay);
    }

    @PreDestroy
    void destroy() {
        Instances.hookManager = NoopHookManager.INSTANCE;
        classInstrumentationJob.cancel();
    }


    private IMethodHook getHook(Class<?> clazz, String methodSignature) {
        try {
            val hook = hooks.get(clazz).get(methodSignature);
            return hook == null ? NoopMethodHook.INSTANCE : hook;
        } catch (ExecutionException e) {
            //Should in theory never occur, as we only call new ConcurrentHashMap() in the cache loader
            log.error("Error in cache", e);
            return NoopMethodHook.INSTANCE;
        }
    }


    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        for (Class<?> clazz : newClasses) {
            pendingClasses.put(clazz, Boolean.TRUE);
        }
        selfMonitorQueueSize();
    }

    @EventListener
    private void configEventListener(InspectitConfigChangedEvent ev) {
        InternalSettings newInternal = ev.getNewConfig().getInstrumentation().getInternal();
        classInstrumentationJob.setBatchSizes(newInternal.getClassConfigurationCheckBatchSize());
        classInstrumentationJob.setInterBatchDelay(newInternal.getInterBatchDelay());
    }

    @EventListener
    private void instrumentationConfigEventListener(InstrumentationConfigurationChangedEvent ev) {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            pendingClasses.put(clazz, Boolean.TRUE);
        }
        selfMonitorQueueSize();
    }

    private void checkClassesForConfigurationUpdates(int batchSize) {

        Stopwatch watch = Stopwatch.createStarted();

        int checkedClassesCount = 0;

        Iterator<Class<?>> queueIterator = pendingClasses.asMap().keySet().iterator();
        while (queueIterator.hasNext() && checkedClassesCount < batchSize) {
            Class<?> clazz = queueIterator.next();
            queueIterator.remove();
            try {
                updateHooksForClass(clazz);
            } catch (Throwable e) {
                log.error("Error checking for hook configuration updates for {}", clazz.getName(), e);
            }
            checkedClassesCount++;
        }
        if (checkedClassesCount > 0) {
            log.debug("Checked hook configuration of {} classes in {} ms, {} classes left to check",
                    checkedClassesCount, watch.elapsed(TimeUnit.MILLISECONDS), pendingClasses.size());
        }

        selfMonitorQueueSize();
    }

    private void updateHooksForClass(Class<?> clazz) {
        Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);

        ConcurrentHashMap<String, MethodHook> activeHooks;
        try {
            activeHooks = hooks.get(clazz);
        } catch (Exception e) {
            //should actually never occur
            log.error("Error updating method hook", e);
            return;
        }

        deactivateRemovedHooks(clazz, hookConfigs, activeHooks);
        addOrReplaceHoks(clazz, hookConfigs, activeHooks);

    }

    private void addOrReplaceHoks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeHooks) {
        hookConfigs.forEach((method, config) -> {
            String signature = CommonUtils.getSignature(method);
            MethodHookConfiguration previous = Optional.ofNullable(activeHooks.get(signature))
                    .map(MethodHook::getSourceConfiguration)
                    .orElse(null);
            if (!Objects.equals(config, previous)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding/updating hook for {} of {}", signature, clazz.getName());
                }
                try {
                    activeHooks.put(signature, hookGenerator.buildHook(clazz, method, config));
                } catch (Throwable t) {
                    log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName());
                    activeHooks.remove(signature);
                }
            }
        });
    }

    private void deactivateRemovedHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeHooks) {
        Set<String> hookedMethodSignature = hookConfigs.keySet().stream().map(m -> CommonUtils.getSignature(m)).collect(Collectors.toSet());
        if (log.isDebugEnabled()) {
            activeHooks.keySet().stream()
                    .filter(signature -> !hookedMethodSignature.contains(signature))
                    .forEach(sig -> log.debug("Removing hook for {} of {}", sig, clazz.getName()));
        }
        //remove hooks of methods which have been deinstrumented
        activeHooks.keySet().removeIf(signature -> !hookedMethodSignature.contains(signature));
    }


    @EventListener(classes = {InspectitConfigChangedEvent.class},
            condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorQueueSize() {
        if (selfMonitoring.isSelfMonitoringEnabled()) {
            val measure = selfMonitoring.getSelfMonitoringMeasureLong(
                    "instrumentation-hooking-queue-size",
                    "The number of pending classes inspectIT has to check if they require instrumentation updates",
                    "classes",
                    Aggregation.LastValue::create);
            selfMonitoring.recordMeasurement(measure, pendingClasses.size());
        }
    }
}
