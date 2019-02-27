package rocks.inspectit.oce.core.instrumentation.hook;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation for {@link rocks.inspectit.oce.bootstrap.instrumentation.IHookManager}.
 * However, this class does not directly implement the interface to avoid issues with spring annotation scanning.
 * Instead it assigns a lambda referring to HookManager{@link #getHook(Class, String)} to {@link Instances#hookManager}.
 */
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

    private final LoadingCache<Class<?>, ConcurrentHashMap<String, MethodHook>> hooks = CacheBuilder.newBuilder().weakKeys().build(
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
    final Cache<Class<?>, Boolean> pendingClasses =
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

    /**
     * Actual implementation for {@link rocks.inspectit.oce.bootstrap.instrumentation.IHookManager#getHook(Class, String)}.
     *
     * @param clazz           the name of the class to which the method to query the hook for belongs
     * @param methodSignature the signature of the method in the form of name(parametertype,parametertype,..)
     * @return
     */
    private IMethodHook getHook(Class<?> clazz, String methodSignature) {
        val hook = hooks.getUnchecked(clazz).get(methodSignature);
        return hook == null ? NoopMethodHook.INSTANCE : hook;
    }


    /**
     * When classes are newly loaded, their configuration needs to be checked.
     *
     * @param newClasses the set of newly discovered classes
     */
    @Override
    public void onNewClassesDiscovered(Set<Class<?>> newClasses) {
        addClassesToQueue(newClasses);
    }

    /**
     * When the configuration changes we need to recheck all classes.
     *
     * @param ev
     */
    @EventListener
    @SuppressWarnings("unchecked")
    private void instrumentationConfigEventListener(InstrumentationConfigurationChangedEvent ev) {
        List<Class<?>> classes = Arrays.asList(instrumentation.getAllLoadedClasses());
        addClassesToQueue(classes);
    }

    private void addClassesToQueue(Collection<Class<?>> classes) {
        for (Class<?> clazz : classes) {
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


    /**
     * Takes batch-size classes from the queue and updates the hooks where required.
     *
     * @param batchSize the number of classes to process in this batch.
     */
    private void checkClassesForConfigurationUpdates(int batchSize) {

        try (val sm = selfMonitoring.withDurationSelfMonitoring("hook-configuration")) {
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
        }

        selfMonitorQueueSize();
    }

    private void updateHooksForClass(Class<?> clazz) {
        Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);

        val activeClassHooks = hooks.getUnchecked(clazz);

        deactivateRemovedHooks(clazz, hookConfigs, activeClassHooks);
        addOrReplaceHooks(clazz, hookConfigs, activeClassHooks);

    }

    private void addOrReplaceHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeClassHooks) {
        hookConfigs.forEach((method, config) -> {
            String signature = CommonUtils.getSignature(method);
            MethodHookConfiguration previous = Optional.ofNullable(activeClassHooks.get(signature))
                    .map(MethodHook::getSourceConfiguration)
                    .orElse(null);
            if (!Objects.equals(config, previous)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding/updating hook for {} of {}", signature, clazz.getName());
                }
                try {
                    activeClassHooks.put(signature, hookGenerator.buildHook(clazz, method, config));
                } catch (Throwable t) {
                    log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName());
                    activeClassHooks.remove(signature);
                }
            }
        });
    }

    private void deactivateRemovedHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeClassHooks) {
        Set<String> hookedMethodSignatures = hookConfigs.keySet().stream()
                .map(CommonUtils::getSignature)
                .collect(Collectors.toSet());
        if (log.isDebugEnabled()) {
            activeClassHooks.keySet().stream()
                    .filter(signature -> !hookedMethodSignatures.contains(signature))
                    .forEach(sig -> log.debug("Removing hook for {} of {}", sig, clazz.getName()));
        }
        //remove hooks of methods which have been deinstrumented
        activeClassHooks.keySet().removeIf(signature -> !hookedMethodSignatures.contains(signature));
    }


    @EventListener(classes = {InspectitConfigChangedEvent.class},
            condition = "!#root.event.oldConfig.selfMonitoring.enabled")
    private void selfMonitorQueueSize() {
        selfMonitoring.recordMeasurement("instrumentation-hooking-queue-size", pendingClasses.size());
    }
}
