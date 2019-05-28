package rocks.inspectit.ocelot.core.instrumentation.hook;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final LoadingCache<Class<?>, ConcurrentHashMap<String, MethodHook>> hooks = CacheBuilder.newBuilder().weakKeys().build(
            new CacheLoader<Class<?>, ConcurrentHashMap<String, MethodHook>>() {
                @Override
                public ConcurrentHashMap<String, MethodHook> load(Class<?> key) throws Exception {
                    return new ConcurrentHashMap<>();
                }
            });


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
        val hook = hooks.getUnchecked(clazz).get(methodSignature);
        return hook == null ? NoopMethodHook.INSTANCE : hook;
    }

    public void updateHooksForClass(Class<?> clazz) {
        try (val sm = selfMonitoring.withDurationSelfMonitoring("HookManager")) {
            Map<MethodDescription, MethodHookConfiguration> hookConfigs = configResolver.getHookConfigurations(clazz);

            val activeClassHooks = hooks.getUnchecked(clazz);

            deactivateRemovedHooks(clazz, hookConfigs, activeClassHooks);
            addOrReplaceHooks(clazz, hookConfigs, activeClassHooks);
        }
    }

    private void addOrReplaceHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeClassHooks) {
        hookConfigs.forEach((method, config) -> {
            String signature = CoreUtils.getSignature(method);
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
                    log.error("Error generating hook for {} of {}. Method will not be hooked.", signature, clazz.getName(), t);
                    activeClassHooks.remove(signature);
                }
            }
        });
    }

    private void deactivateRemovedHooks(Class<?> clazz, Map<MethodDescription, MethodHookConfiguration> hookConfigs, ConcurrentHashMap<String, MethodHook> activeClassHooks) {
        Set<String> hookedMethodSignatures = hookConfigs.keySet().stream()
                .map(CoreUtils::getSignature)
                .collect(Collectors.toSet());
        if (log.isDebugEnabled()) {
            activeClassHooks.keySet().stream()
                    .filter(signature -> !hookedMethodSignatures.contains(signature))
                    .forEach(sig -> log.debug("Removing hook for {} of {}", sig, clazz.getName()));
        }
        //remove hooks of methods which have been deinstrumented
        activeClassHooks.keySet().removeIf(signature -> !hookedMethodSignatures.contains(signature));
    }
}
