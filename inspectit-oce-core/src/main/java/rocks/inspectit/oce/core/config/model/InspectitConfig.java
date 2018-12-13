package rocks.inspectit.oce.core.config.model;

import lombok.Data;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import rocks.inspectit.oce.core.config.ConfigurationCenter;
import rocks.inspectit.oce.core.config.model.config.ConfigSettings;

/**
 * Root element of the configuration model for inspectIT.
 * The loading of the configuration is managed by the {@link ConfigurationCenter}.
 * <p>
 * The default values and the structure of the configuration can be found in the /config/default.yml file.
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

    public static InspectitConfig createFromEnvironment(Environment env) {
        return Binder.get(env).bind("inspectit", InspectitConfig.class).get();
    }
}
