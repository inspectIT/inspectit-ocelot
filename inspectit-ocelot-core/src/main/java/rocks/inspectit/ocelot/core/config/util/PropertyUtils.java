package rocks.inspectit.ocelot.core.config.util;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author Jonas Kunz
 */
public class PropertyUtils {

    /**
     * Reads the given YAML resources into a {@link Properties} Object according to Spring rules.
     *
     * @param resources the resources to load
     *
     * @return the generated {@link Properties} object
     */
    public static Properties readYamlFiles(Resource... resources) {
        YamlPropertiesFactoryBean properties = new YamlPropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setResources(resources);
        return properties.getObject();
    }

    /**
     * Reads the given .properties resources into a {@link Properties} Object according to Spring rules.
     *
     * @param resources the resources to load
     *
     * @return the generated {@link Properties} object
     */
    public static Properties readPropertyFiles(Resource... resources) throws IOException {
        PropertiesFactoryBean properties = new PropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setLocations(resources);
        return properties.getObject();
    }

    /**
     * Reads the given YAML or JSON resource into a {@link Properties} Object according to Spring rules.
     * The method will try to parse the resource as YAML, as YAML is a superset of JSON.
     *
     * @param yamlOrJson the YAML or JSON string to read
     *
     * @return the generated {@link Properties} object
     */
    public static Properties readYamlOrJson(String yamlOrJson) {
        ByteArrayResource resource = new ByteArrayResource(yamlOrJson.getBytes(StandardCharsets.UTF_8));
        return readYamlFiles(resource);
    }


}


