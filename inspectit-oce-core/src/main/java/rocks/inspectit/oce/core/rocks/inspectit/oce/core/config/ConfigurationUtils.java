package rocks.inspectit.oce.core.rocks.inspectit.oce.core.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.AbstractResource;

import java.util.Properties;

/**
 * @author Jonas Kunz
 */
public class ConfigurationUtils {

    /**
     * Reads the given YAML resources into a Properties Object according to Spring rules.
     * @param resources the resources to load
     * @return the generated Proeprties object
     */
    public static Properties readYamlsAsProperties(AbstractResource... resources) {
        YamlPropertiesFactoryBean properties = new YamlPropertiesFactoryBean();
        properties.setResources(resources);
        return properties.getObject();
    }

}
