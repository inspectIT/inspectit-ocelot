package rocks.inspectit.oce.core.config.model;

import lombok.Data;
import rocks.inspectit.oce.core.config.model.config.ConfigSettings;

import javax.validation.constraints.Min;

/**
 * Root element of the configuration model for inspectIT.
 * The loading of the configuration is managed by the {@link rocks.inspectit.oce.core.config.InspectitEnvironment}.
 * <p>
 * The default values and the structure of the configuration can be found in the /config/default.yml file.
 * Note that in configuration files fields have tobe referred to in kebab-case instead of camelCase!
 * <p>
 * Instances of this class should be treated as values, therefore the setters should never be called!
 * The setters have to be there to work with  the {@link org.springframework.boot.context.properties.bind.Binder}.
 *
 * @author Jonas Kunz
 */
@Data
public class InspectitConfig {

    /**
     * The (symbolic) name of the service being instrumented
     */
    String serviceName;

    /**
     * Defines all configuration sources.
     */
    ConfigSettings config;

    /**
     * Defines how many threads inspectIT may start for its internal tasks.
     */
    @Min(1)
    int threadPoolSize;

}
