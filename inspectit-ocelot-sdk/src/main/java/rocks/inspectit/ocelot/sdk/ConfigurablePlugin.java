package rocks.inspectit.ocelot.sdk;

import rocks.inspectit.ocelot.config.model.InspectitConfig;

/**
 * Base class for all plugins.
 * Plugins need to have the {@link OcelotPlugin} annotation, otherwise they won't be loaded.
 *
 * @param <T> the configuration class for this plugin, see {@link #getConfigurationClass()}
 */
public interface ConfigurablePlugin<T> {

    /**
     * Invoked when the plugin is first loaded.
     * If the user configuration is not valid, e.g. if a a field annotated with @NotBlank is blank, this method will not be called
     * until the configuration is valid.
     *
     * @param inspectitConfig the inspectit main configuration
     * @param pluginConfig    the plugin specific validated configuration.
     */
    void start(InspectitConfig inspectitConfig, T pluginConfig);

    /**
     * Invoked when any configuration property has changed after {@link #start(InspectitConfig, Object)} has already been invoked.
     * <p>
     * In this method, the plugin should react to configuration changed (E.g. by starting or stopping an exporter).
     * If the user configuration is not valid, e.g. if a a field annotated with @NotBlank is blank, this method will not be called
     * until the configuration is valid.
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
     * Invoked when the ocelot agent shuts down.
     */
    default void destroy() {
    }
}
