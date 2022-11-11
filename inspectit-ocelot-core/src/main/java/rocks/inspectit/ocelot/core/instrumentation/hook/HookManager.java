package rocks.inspectit.ocelot.core.instrumentation.hook;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.opencensus.common.Scope;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopMethodHook;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation for {@link IHookManager}.
 * However, this class does not directly implement the interface to avoid issues with spring annotation scanning.
 * Instead it assigns a lambda referring to HookManager{@link #getHook(Class, String)} to {@link Instances#hookManager}.
 */
@Slf4j
@Service
public class HookManager {

    /**
     * Component named used for self monitoring metrics
     */
    private static final String LAZY_LOADING_HOOK_COMPONENT_NAME = "hookmanager-lazy-hooking";

    /**
     * Thread local flag for marking the current thread that it is currently in the execution/scope of agent actions.
     * This is used to prevent an endless action recursion in case an instrumented action is invoked within another
     * action. In that case, the instrumentation has to be suppressed.
     */
    public static final ThreadLocal<Boolean> RECURSION_GATE = ThreadLocal.withInitial(() -> false);

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private MethodHookGenerator hookGenerator;

    @Autowired
    private InspectitEnvironment env;

    /**
     * Holds the currently active hooks.
     * The keys of the map are Classes for which hooks are present.
     * The value is a map for each class which maps method signatures to the active hooks.
     * <p>
     * This map is not modifiable! Instead, the entire map is replaced when an update occurs.
     * <p>
     * The keys of this map (the instrumented classes) must be weakly referenced to prevent memory leaks.
     */
    private volatile Map<Class<?>, Map<String, MethodHook>> hooks = Collections.emptyMap();

    /**
     * Flag indicates that lazy loading of hooks is enabled. This is only possible if the configuration value
     * {@code inspectit.instrumentation.internal.async} is {@code false}.
     * If lazy hook loading is enabled, hooks will be generated on the fly while an instrumentation method asks for hooks.
     * Only one attempt for lazy loading hooks will be performed! If no hooks are generated, e.g. due to a missing or invalid
     * configuration, no further attempts will be performed. Assumption is that updated hook configurations will be
     * considered during regular asynchronous updates.
     */
    private boolean isLazyHookingEnabled;

    /**
     * Holds the latest lazy loaded hooks.
     * All lazy loaded hooks will be merged to regular {@link HookManager#hooks} map during next {@link  HookUpdate}.
     */
    private final Map<Class<?>, Map<String, MethodHook>> lazyLoadedHooks = new ConcurrentHashMap<>();

    /**
     * Saves for which classes the hooks were lazy loaded and acts as a lock for all further attempts.
     * Lazy loading hooks will only be done once per class!
     */
    private final Set<Class<?>> lazyHookingPerformed = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void init() {
        isLazyHookingEnabled = !env.getCurrentConfig().getInstrumentation().getInternal().isAsync();
        Instances.hookManager = this::getHook;
    }

    @PreDestroy
    void destroy() {
        Instances.hookManager = NoopHookManager.INSTANCE;
    }

    /**
     * Actual implementation for {@link IHookManager#getHook(Class, String)}.
     *
     * @param clazz           the name of the class to which the method to query the hook for belongs
     * @param methodSignature the signature of the method in the form of name(parametertype, parametertype,..)
     *
     * @return the method hook for the specified method
     */
    @VisibleForTesting
    IMethodHook getHook(Class<?> clazz, String methodSignature) {
        if (!RECURSION_GATE.get()) {
            Map<String, MethodHook> methodHooks = hooks.get(clazz);
            if (isLazyHookingEnabled && methodHooks == null) {
                methodHooks = lazyHookGeneration(clazz);
            }
            if (methodHooks != null) {
                MethodHook hook = methodHooks.get(methodSignature);
                if (hook != null) {
                    return hook;
                }
            }
        }

        return NoopMethodHook.INSTANCE;
    }

    /**
     * Creates {@link  MethodHook}s lazy if hooks are not yet created for an instrumented class.
     * Lazy loaded hooks are merged to {@link HookManager#hooks} map during next regular {@link HookUpdate}.
     * This method will only be called during JVM ramp up phase as long as not all classes are loaded.
     *
     * @param clazz the name of the class to which the method to query the hook for belongs
     *
     * @return the method hooks for the specified class if a valid hook configurations is available, null otherwise
     */
    private Map<String, MethodHook> lazyHookGeneration(Class<?> clazz) {
        if (lazyHookingPerformed.contains(clazz)) {
            return lazyLoadedHooks.get(clazz);
        }
        synchronized (clazz) {
            try (Scope sm = selfMonitoring.withDurationSelfMonitoring(LAZY_LOADING_HOOK_COMPONENT_NAME)) {
                Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);

                HashMap<String, MethodHook> lazyHooks = Maps.newHashMap();
                hookConfigs.forEach((method, config) -> {
                    String signature = CoreUtils.getSignature(method);
                    try {
                        MethodHook methodHook = hookGenerator.buildHook(clazz, method, config);
                        lazyHooks.put(signature, methodHook);
                        if (log.isDebugEnabled()) {
                            log.debug("Lazy loading hooks for {} of {}.", signature, clazz.getName());
                        }
                    } catch (Throwable throwable) {
                        log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName(), throwable);
                    }
                });

                if (!lazyHooks.isEmpty()) {
                    Map<String, MethodHook> methodHooks = hooks.get(clazz);
                    if (methodHooks != null) {
                        // It seems async hooking triggered from InstrumentationTrigger kicked in between
                        // Drop lazy hooks and go on
                        return methodHooks;
                    } else {
                        lazyLoadedHooks.put(clazz, lazyHooks);
                        // Lock lazy loading hooks for this class. We only try to lazy hooking once
                        lazyHookingPerformed.add(clazz);
                        return lazyHooks;
                    }
                }
                return null;

            }
        }
    }

    /**
     * Starts an update of the hook configurations.
     * The returned HookUpdate copies all currently active hooks and resets them.
     * The configuration can be changed as needed and activated atomically using {@link HookUpdate#commitUpdate()}.
     *
     * @return the object for manipulating one or more hooks.
     */
    public HookUpdate startUpdate() {
        return new HookUpdate();
    }

    /**
     * A {@link HookUpdate} instance represents a (potential) change to apply to the {@link HookManager}.
     * Hooks for any class can be updated by calling {@link #updateHooksForClass(Class)}. This updates
     * are stored in the {@link HookUpdate} and are NOT directly active on the {@link HookManager}.
     * <p>
     * All updates are applied atomically by calling {@link #commitUpdate()}.
     * Calling {@link #commitUpdate()} also causes all hooks to be reset, meaning that hook actions which have been
     * deactivated due to runtime exceptions are reactivated.
     */
    public class HookUpdate {

        /**
         * Initially, this map is a copy of {@link #hooks}, but with all hooks reset.
         * This map is then mutated by calling {@link #updateHooksForClass(Class)},
         * before it finally replaces {@link #hooks} after a call to {@link #commitUpdate()}.
         * <p>
         * The structure of the map is the same as for {@link #hooks}.
         */
        private final WeakHashMap<Class<?>, Map<String, MethodHook>> newHooks;

        private boolean committed = false;

        /**
         * Copies the currently active hooks into a mutable, local state.
         * The hooks are reset when copied to re-enable actions which have been deactivated due to runtime errors.
         */
        private HookUpdate() {
            try (Scope sm = selfMonitoring.withDurationSelfMonitoring("hookmanager-copy-existing-hooks")) {

                // Merge regular and lazy loaded hooks. Regular hooks take precedence
                WeakHashMap<Class<?>, Map<String, MethodHook>> mergedHooks = Stream.of(hooks, lazyLoadedHooks)
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, WeakHashMap::new));

                newHooks = new WeakHashMap<>();
                for (Map.Entry<Class<?>, Map<String, MethodHook>> existingMethodHooks : mergedHooks.entrySet()) {
                    HashMap<String, MethodHook> newMethodHooks = new HashMap<>();
                    existingMethodHooks.getValue()
                            .forEach((signature, hook) -> newMethodHooks.put(signature, hook.getResetCopy()));
                    newHooks.put(existingMethodHooks.getKey(), newMethodHooks);
                }
            }
        }

        /**
         * Adds, removes or updates hooks for the given class based on the current instrumentation configuration.
         *
         * @param clazz the class to check
         */
        public void updateHooksForClass(Class<?> clazz) {
            ensureNotCommitted();
            try (Scope sm = selfMonitoring.withDurationSelfMonitoring("hookmanager-update-class")) {
                Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);
                removeObsoleteHooks(clazz, hookConfigs.keySet());
                addOrReplaceHooks(clazz, hookConfigs);
            }
        }

        /**
         * Activates all changes made via {@link #updateHooksForClass(Class)} and reenables all disables actions of all hooks
         * on the {@link HookManager}.
         * After this method is called, {@link #getHook(Class, String)} will return the updated hooks.
         */
        public void commitUpdate() {
            ensureNotCommitted();
            hooks = newHooks;
            // Remove all updated hooks from lazy loaded map
            if (lazyLoadedHooks.size() > 0) {
                lazyLoadedHooks.forEach((k, v) -> {
                    if (hooks.containsKey(k)) {
                        lazyLoadedHooks.remove(k);
                    }
                });
            }
            committed = true;
        }

        private void ensureNotCommitted() {
            if (committed) {
                throw new IllegalStateException("Update has already been committed!");
            }
        }

        private Optional<MethodHook> getCurrentHook(Class<?> declaringClass, String methodSignature) {
            Map<String, MethodHook> methodHooks = newHooks.get(declaringClass);
            return Optional.ofNullable(methodHooks).map(myHooks -> myHooks.get(methodSignature));
        }

        private void setHook(Class<?> declaringClass, String methodSignature, MethodHook newHook) {
            newHooks.computeIfAbsent(declaringClass, (v) -> new HashMap<>()).put(methodSignature, newHook);
        }

        private void removeHook(Class<?> declaringClass, String methodSignature) {
            Map<String, MethodHook> methodHooks = newHooks.get(declaringClass);
            if (methodHooks != null) {
                methodHooks.remove(methodSignature);
                if (methodHooks.isEmpty()) {
                    newHooks.remove(declaringClass);
                }
            }
        }

        /**
         * Adds and updates existing hooks for a given class based on the desired configuration.
         * <p>
         * For each provided method a hook is created, if it does not already exist.
         * If a hook for a method already exists, it is checked if the configuration for the given hook has changed.
         * If this is the case, a hook is generated and the old one is replaced.
         * <p>
         * If the hook generation fails for any reason, any previous hook for the method is removed.
         *
         * @param clazz       the class for whose methods the hooks shall be updated
         * @param hookConfigs Maps method signature to the hook configuration which shall be used for the given method
         */
        private void addOrReplaceHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs) {
            hookConfigs.forEach((method, newConfig) -> {
                String signature = CoreUtils.getSignature(method);
                MethodHookConfiguration oldConfig = getCurrentHook(clazz, signature).map(MethodHook::getSourceConfiguration)
                        .orElse(null);
                if (!Objects.equals(newConfig, oldConfig)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding/updating hook for {} of {}", signature, clazz.getName());
                    }
                    try {
                        setHook(clazz, signature, hookGenerator.buildHook(clazz, method, newConfig));
                    } catch (Throwable t) {
                        log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName(), t);
                        removeHook(clazz, signature);
                    }
                }
            });
        }

        /**
         * Removes hooks for methods which have previously been instrumented but are not instrumented in the current configuration.
         * E.g. if a user instruments MyClass.myMethod but then later removes this instrumentation, the hooks also needs to be removed.
         *
         * @param clazz                 the class for which obsolete hooks shall be removed
         * @param methodHooksToPreserve the set of methods for which the hooks should NOT be removed
         */
        private void removeObsoleteHooks(Class<?> clazz, Set<MethodDescription> methodHooksToPreserve) {
            Set<String> methodsWithHooksInNewConfiguration = methodHooksToPreserve.stream()
                    .map(CoreUtils::getSignature)
                    .collect(Collectors.toSet());

            Set<String> existingHooks = new HashSet<>(newHooks.getOrDefault(clazz, Collections.emptyMap()).keySet());
            for (String method : existingHooks) {
                if (!methodsWithHooksInNewConfiguration.contains(method)) {
                    log.debug("Removing hook for {} of {}", method, clazz.getName());
                    removeHook(clazz, method);
                }
            }
        }
    }
}
