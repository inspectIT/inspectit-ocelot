package rocks.inspectit.ocelot.core.instrumentation.hook;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopHookManager;
import rocks.inspectit.ocelot.bootstrap.instrumentation.noop.NoopMethodHook;
import rocks.inspectit.ocelot.core.instrumentation.config.InstrumentationConfigurationResolver;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.selfmonitoring.SelfMonitoringService;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation for {@link IHookManager}.
 * However, this class does not directly implement the interface to avoid issues with spring annotation scanning.
 * Instead it assigns a lambda referring to HookManager{@link #getHook(Class, String)} to {@link Instances#hookManager}.
 */
@Slf4j
@Service
public class HookManager {

    @Autowired
    private InstrumentationConfigurationResolver configResolver;

    @Autowired
    private SelfMonitoringService selfMonitoring;

    @Autowired
    private MethodHookGenerator hookGenerator;

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

    @PostConstruct
    void init() {
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
     * @param methodSignature the signature of the method in the form of name(parametertype,parametertype,..)
     *
     * @return
     */
    private IMethodHook getHook(Class<?> clazz, String methodSignature) {
        Map<String, MethodHook> methodHooks = hooks.get(clazz);
        if (methodHooks != null) {
            MethodHook hook = methodHooks.get(methodSignature);
            if (hook != null) {
                return hook;
            }
        }
        return NoopMethodHook.INSTANCE;
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
        private WeakHashMap<Class<?>, Map<String, MethodHook>> newHooks;

        private boolean committed = false;

        /**
         * Copies the currently active hooks into a mutable, local state.
         * The hooks are reset when copied to reenable actions which have been deactivated due to runtime errors.
         */
        private HookUpdate() {
            try (val sm = selfMonitoring.withDurationSelfMonitoring("hookmanager-copy-existing-hooks")) {
                newHooks = new WeakHashMap<>();
                for (Map.Entry<Class<?>, Map<String, MethodHook>> existingMethodHooks : hooks.entrySet()) {
                    HashMap<String, MethodHook> newMethodHooks = new HashMap<>();
                    existingMethodHooks.getValue()
                            .forEach((signature, hook) -> newMethodHooks.put(signature, hook.getResettedCopy()));
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
            try (val sm = selfMonitoring.withDurationSelfMonitoring("hookmanager-update-class")) {
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
            committed = true;
        }

        private void ensureNotCommitted() {
            if (committed) {
                throw new IllegalStateException("Update has already been committed!");
            }
        }

        private Optional<MethodHook> getCurrentHook(Class<?> declaringClass, String methodSignature) {
            Map<String, MethodHook> methodHooks = newHooks.get(declaringClass);
            return Optional.ofNullable(methodHooks).map(hooks -> hooks.get(methodSignature));
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
