package rocks.inspectit.ocelot.sdk;

import rocks.inspectit.ocelot.config.model.InspectitConfig;

/**
 * Base class for all plugins.
 * To be loaded plugins also need to have the {@link OcelotPlugin} annotation.
 *
 * @param <T> the configuration class for this plugin, see {@link #getConfigurationClass()}
 */
public interface ConfigurablePlugin<T> {

    /**
     * Invoked when (a) the plugin is first loaded or (b) any configuration property has changed.
     * In this method, the plugin should react to configuration changed (E.g. by starting or stopping an exporter).
     * If the user configuration is not valid, e.g. if a a field annotated with @NotBlank is blank, this method will not be called
     * until the configuration si fixed.
     *
     * @param inspectitConfig the inspectit main configuration
     * @param pluginConfig    the plugin specific validated configuration.
     */
    void update(InspectitConfig inspectitConfig, T pluginConfig);

    /**
     * Plugins can have specialized configurations.
     * Configurations are defined as a simple POJO with public getters and setters for their properties and a default constructor.
     * In addition javax.validation annotations can be used to validate the properties.
     *
     * @return the class of the configuration POJO
     */
    Class<T> getConfigurationClass();

    /**
     * Invoked when the ocelot shuts down.
     */
    default void destroy() {
    }
}
