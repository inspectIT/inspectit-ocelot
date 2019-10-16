package rocks.inspectit.ocelot.core.plugins;

import lombok.Getter;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.plugins.PluginSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.sdk.ConfigurablePlugin;

import java.util.Objects;
import java.util.Optional;

/**
 * Container for a running {@link ConfigurablePlugin}.
 */
class LoadedPlugin {

    /**
     * The name of the plugin which is used with PLUGIN_CONFIG_PREFIX to bind the configuration.
     */
    @Getter
    private final String name;

    /**
     * The actual plugin instance
     */
    private final ConfigurablePlugin plugin;

    /**
     * The value of{@link ConfigurablePlugin#getConfigurationClass()}, stored to prevent inconsistencies by faulting plugins.
     */
    private final Class<?> configClass;

    /**
     * Stores the last config with which plugin.update was called on {@link #plugin}.
     * This is used to ensure that update is only called again if anything has changed.
     */
    private InspectitConfig currentInspectitConfig;

    /**
     * Stores the last config with which plugin.update was called on {@link #plugin}.
     * This is used to ensure that update is only called again if anything has changed.
     * This object is an Instance of the plugins specific configuration,
     * parsed from inspectit.plugin.{pluginname}.
     */
    private Object currentPluginConfig;

    /**
     * True, if the plugins {@link ConfigurablePlugin#start(InspectitConfig, Object)} method has already been called.
     * This means that subsequent updates will only invoke {@link ConfigurablePlugin#update(InspectitConfig, Object)}
     */
    boolean initialized = false;

    public LoadedPlugin(ConfigurablePlugin plugin, String name) {
        this.plugin = plugin;
        configClass = plugin.getConfigurationClass();
        this.name = name;
    }

    /**
     * Reloads the configuration from the current property sources.
     * If either the {@link InspectitConfig} or the plugins specific configuration has changed,
     * plugin.update will be invoked.
     *
     * @param env the InspectitEnvironment which owns the property sources and the active {@link InspectitConfig}
     */
    public void updateConfiguration(InspectitEnvironment env) {
        InspectitConfig newInspectitConfig = env.getCurrentConfig();
        if (configClass != null) {
            Optional<?> configOpt = env.loadAndValidateFromProperties(PluginSettings.PLUGIN_CONFIG_PREFIX + name, configClass);
            //no need to print an error if the Optional is not present, this is already done by loadAndValidateFromProperties
            if (configOpt.isPresent()) {
                Object newPluginConfig = configOpt.get();
                if (!Objects.equals(newInspectitConfig, currentInspectitConfig) || !Objects.equals(currentPluginConfig, newPluginConfig)) {
                    currentPluginConfig = newPluginConfig;
                    currentInspectitConfig = newInspectitConfig;
                    startOrUpdate(newInspectitConfig, currentPluginConfig);
                }
            }
        } else {
            if (!Objects.equals(newInspectitConfig, currentInspectitConfig)) {
                currentInspectitConfig = newInspectitConfig;
                startOrUpdate(newInspectitConfig, null);
            }
        }
    }

    /**
     * If {@link ConfigurablePlugin#start(InspectitConfig, Object)} has not been invoked yet,
     * it will be invoked with the given arguments. Otherwise
     * {@link ConfigurablePlugin#update(InspectitConfig, Object)} (InspectitConfig, Object)} is called.
     *
     * @param inspectitConf the inspectit configuration to pass to the plugin
     * @param pluginConfig  the plugins configuration to pass to the plugin
     */
    @SuppressWarnings("unchecked")
    private void startOrUpdate(InspectitConfig inspectitConf, Object pluginConfig) {
        if (initialized) {
            withPluginClassloader(() -> plugin.update(inspectitConf, pluginConfig));
        } else {
            initialized = true;
            withPluginClassloader(() -> plugin.start(inspectitConf, pluginConfig));
        }
    }

    /**
     * Executes the given function, but sets the context classloader of the thread to the plugins classloader.
     * This ensures that class-path scanning works correctly if used by any plugin.
     *
     * @param task the function to execute
     */
    public void withPluginClassloader(Runnable task) {
        Thread thread = Thread.currentThread();
        ClassLoader prevContextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(plugin.getClass().getClassLoader());
        try {
            task.run();
        } finally {
            thread.setContextClassLoader(prevContextClassLoader);
        }
    }

    /**
     * Destroys the loaded plugin.
     */
    public void destroy() {
        withPluginClassloader(plugin::destroy);
    }
}
