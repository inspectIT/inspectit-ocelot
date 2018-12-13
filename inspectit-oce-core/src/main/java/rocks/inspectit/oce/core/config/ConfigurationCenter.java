package rocks.inspectit.oce.core.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.InspectitConfig;

import javax.annotation.PostConstruct;

/**
 * Compoennt responsible for loading and reloading the inspectit configurations.
 * The configuration is read from the properties of the spring environment
 *
 * @author Jonas Kunz
 */
@Component
public class ConfigurationCenter {

    @Autowired
    ConfigurableEnvironment env;

    @Getter
    private InspectitConfig currentConfiguration;

    /**
     * (Re-)loads the {@link InspectitConfig} from the environemnt.
     * If any changes are detected an event is generated.
     */
    @PostConstruct
    public void reloadConfiguration() {
        currentConfiguration = InspectitConfig.createFromEnvironment(env);
        System.out.println(currentConfiguration);
        //TODO: compare with previous config: if any changes are present send an event
    }
}
