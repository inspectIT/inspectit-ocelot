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
     * @return
     */
    private IMethodHook getHook(Class<?> clazz, String methodSignature) {
        Map<String, MethodHook> classHooks = hooks.get(clazz);
        if (classHooks != null) {
            MethodHook hook = classHooks.get(methodSignature);
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


    public class HookUpdate {

        private WeakHashMap<Class<?>, Map<String, MethodHook>> newHooks;

        private boolean isCommitted = false;

        /**
         * Copies the currently active hooks into a mutable, local state.
         * The hooks are reset when copied to reenable actions which have been deactivated due to runtime errors.
         */
        private HookUpdate() {
            try (val sm = selfMonitoring.withDurationSelfMonitoring("HookManager")) {
                newHooks = new WeakHashMap<>();
                for (Map.Entry<Class<?>, Map<String, MethodHook>> existingClassHooks : hooks.entrySet()) {
                    HashMap<String, MethodHook> newClassHooks = new HashMap<>();
                    existingClassHooks.getValue().forEach((signature, hook) -> newClassHooks.put(signature, hook.getResettedCopy()));
                    newHooks.put(existingClassHooks.getKey(), newClassHooks);
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
            try (val sm = selfMonitoring.withDurationSelfMonitoring("HookManager")) {
                Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);
                deactivateRemovedHooks(clazz, hookConfigs);
                addOrReplaceHooks(clazz, hookConfigs);
            }
        }

        public void commitUpdate() {
            ensureNotCommitted();
            hooks = newHooks;
            isCommitted = true;
        }

        private void ensureNotCommitted() {
            if (isCommitted) {
                throw new IllegalStateException("Update has already been committed!");
            }
        }

        private Optional<MethodHook> getCurrentHook(Class<?> declaringClass, String methodSignature) {
            Map<String, MethodHook> classHooks = newHooks.get(declaringClass);
            if (classHooks != null) {
                return Optional.ofNullable(classHooks.get(methodSignature));
            } else {
                return Optional.empty();
            }
        }

        private void setHook(Class<?> declaringClass, String methodSignature, MethodHook newHook) {
            newHooks.computeIfAbsent(declaringClass, (v) -> new HashMap<>())
                    .put(methodSignature, newHook);
        }

        private void removeHook(Class<?> declaringClass, String methodSignature) {
            Map<String, MethodHook> classHooks = newHooks.get(declaringClass);
            if (classHooks != null) {
                classHooks.remove(methodSignature);
                if (classHooks.isEmpty()) {
                    newHooks.remove(declaringClass);
                }
            }
        }


        private void addOrReplaceHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs) {
            hookConfigs.forEach((method, config) -> {
                String signature = CoreUtils.getSignature(method);
                MethodHookConfiguration previous = getCurrentHook(clazz, signature)
                        .map(MethodHook::getSourceConfiguration)
                        .orElse(null);
                if (!Objects.equals(config, previous)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding/updating hook for {} of {}", signature, clazz.getName());
                    }
                    try {
                        setHook(clazz, signature, hookGenerator.buildHook(clazz, method, config));
                    } catch (Throwable t) {
                        log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName(), t);
                        removeHook(clazz, signature);
                    }
                }
            });
        }

        private void deactivateRemovedHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs) {
            Set<String> methodsToHook = hookConfigs.keySet().stream()
                    .map(CoreUtils::getSignature)
                    .collect(Collectors.toSet());

            Set<String> existingHooks = new HashSet<>(newHooks.getOrDefault(clazz, Collections.emptyMap()).keySet());
            for (String method : existingHooks) {
                if (!methodsToHook.contains(method)) {
                    log.debug("Removing hook for {} of {}", method, clazz.getName());
                    removeHook(clazz, method);
                }
            }
        }
    }
}
