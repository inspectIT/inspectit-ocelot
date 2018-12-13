package rocks.inspectit.oce.core.config.filebased;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Jonas Kunz
 */
public class PropertyFileUtils {

    /**
     * Reads the given YAML resources into a Properties Object according to Spring rules.
     *
     * @param resources the resources to load
     * @return the generated Properties object
     */
    public static Properties readYamlFiles(AbstractResource... resources) {
        YamlPropertiesFactoryBean properties = new YamlPropertiesFactoryBean();
        properties.setResources(resources);
        return properties.getObject();
    }

    /**
     * Reads the given .properties resources into a Properties Object according to Spring rules.
     *
     * @param resources the resources to load
     * @return the generated Properties object
     */
    public static Properties readPropertyFiles(AbstractResource... resources) throws IOException {
        PropertiesFactoryBean properties = new PropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setLocations(resources);
        return properties.getObject();
    }

}
