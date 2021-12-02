package rocks.inspectit.ocelot.core.config.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import rocks.inspectit.ocelot.core.config.propertysources.http.RawProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jonas Kunz
 */
public class PropertyUtils {

    /**
     * Reads the given YAML resources into a Properties Object according to Spring rules.
     *
     * @param resources the resources to load
     *
     * @return the generated Properties object
     */
    public static Properties readYamlFiles(Resource... resources) {
        YamlPropertiesFactoryBean properties = new YamlPropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setResources(resources);
        return properties.getObject();
    }

    /**
     * Reads the given .properties resources into a Properties Object according to Spring rules.
     *
     * @param resources the resources to load
     *
     * @return the generated Properties object
     */
    public static Properties readPropertyFiles(Resource... resources) throws IOException {
        PropertiesFactoryBean properties = new PropertiesFactoryBean();
        properties.setSingleton(false);
        properties.setLocations(resources);
        return properties.getObject();
    }

    public static Properties readJsonFile(Resource jsonFile) throws JsonParseException, IOException {
        try (InputStream is = jsonFile.getInputStream()) {
            return readJsonFromStream(is);
        }
    }

    public static Properties readJson(String json) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            return readJsonFromStream(is);
        }
    }

    public static Properties readYaml(String yaml) {
        ByteArrayResource resource = new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8));
        return readYamlFiles(resource);
    }

    static Properties readJsonFromStream(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            Map<String, Object> mapFormat = mapper.readValue(is, new TypeReference<Map<String, Object>>() {
            });
            Properties result = new Properties();
            result.putAll(MapListTreeFlattener.flatten(mapFormat));
            return result;
        } catch (JsonMappingException e) {
            throw new RuntimeException(e); //should not occur as we are mapping to plain maps
        }
    }

    /**
     * Reads the given resources (either JSON or YAML) into a {@link Properties} Object according to Spring rules.
     * The method will first try to parse the resource as JSON. If this fails, it tries to parse YAML.
     *
     * @param rawProperties
     *
     * @return
     */
    public static Properties read(String rawProperties) {
        try {
            return PropertyUtils.readJson(rawProperties);
        } catch (IOException e) {
            return PropertyUtils.readYaml(rawProperties);
        }
    }

    /**
     * Reads the given resources (either JSON or YAML) into a {@link Properties} Object according to Spring rules.
     *
     * @param rawProperties The raw properties String
     * @param mimeType      The MIME type of the properties string. The MIME type must not be null.
     *
     * @return the generated {@link Properties} object
     *
     * @throws IOException
     */
    public static Properties read(String rawProperties, String mimeType) throws IOException {
        return read(rawProperties, ContentType.parse(mimeType));
    }

    /**
     * Reads the given resources (either JSON or YAML) into a {@link Properties} Object according to Spring rules.
     *
     * @param rawProperties The raw properties String
     * @param contentType   The {@link ContentType} type of the properties string. The ContentType must not be null.
     *
     * @return the generated {@link Properties} object
     *
     * @throws IOException
     */
    public static Properties read(String rawProperties, ContentType contentType) throws IOException {

        // depending on the MIME type, call the appropriate parsing method.
        // it is sufficient to only check the {@link ContentType#mimeType} and not the entire {@link ContentType}, as we do not care about the charset so far.
        // The equals-method of ContentType does not reliably work

        // if the MIME type is 'text/plain' or no MIME type is present, try first to readJson and then readYaml
        if (contentType == null || ContentType.TEXT_PLAIN.getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
            return read(rawProperties);
        }

        // otherwise, parse the properties with the appropriate parser

        // JSON
        if (ContentType.APPLICATION_JSON.getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
            return readJson(rawProperties);
        }
        // YAML
        else if (ContentType.parse("application/x-yaml").getMimeType().equalsIgnoreCase(contentType.getMimeType())) {
            return readYaml(rawProperties);
        }
        // other MIME types are not supported.
        else {
            throw new IOException("Failed to read properties. MIME type " + contentType + " is not supported!");
        }
    }

    /**
     * Reads the given resources (either JSON or YAML) into a Properties Object according to Spring rules.
     *
     * @param rawConfiguration The raw properties represented as a {@link RawProperties} with String and {@link ContentType} type
     *
     * @return the generated {@link Properties} object
     *
     * @throws IOException
     */
    public static Properties read(RawProperties rawConfiguration) throws IOException {
        return read(rawConfiguration.getRawProperties(), rawConfiguration.getContentType());
    }
}


