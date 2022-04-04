package rocks.inspectit.ocelot.core.config.util;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

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
     *
     * @throws InvalidPropertiesException if the generated {@link Properties} is invalid
     */
    public static Properties readYamlFiles(Resource... resources) throws InvalidPropertiesException {
        YamlPropertiesFactoryBean properties = new YamlPropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setResources(resources);
        Properties result = properties.getObject();
        validateProperties(result);
        return result;
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
    public static Properties readYaml(String yamlOrJson) throws InvalidPropertiesException {
        ByteArrayResource resource = new ByteArrayResource(yamlOrJson.getBytes(StandardCharsets.UTF_8));
        Properties result = readYamlFiles(resource);
        validateProperties(result);
        return result;
    }

    /**
     * Validates the given  {@link Properties}. It is assumed that a property is invalid if it contains a ":" character
     * in its key and the value itself is empty.
     *
     * @param properties the {@link Properties} to validate
     *
     * @throws InvalidPropertiesException if the given {@link Properties} is invalid
     */
    private static void validateProperties(Properties properties) throws InvalidPropertiesException {
        // filter invalid entries, i.e., entries whose key contain a colon (":") and the value is empty
        List<Map.Entry<Object, Object>> invalidEntries = properties.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toString().contains(":") && entry.getValue().toString().isEmpty())
                .collect(Collectors.toList());
        if (!invalidEntries.isEmpty()) {
            throw new InvalidPropertiesException(String.format("Properties contain invalid YAML or JSON. Make sure that valid YAML or JSON is used. For JSON, all keys must be quoted, otherwise the value is parsed as part of the key. Invalid properties: %s", Arrays
                    .toString(invalidEntries.toArray())));
        }
    }

}


