package rocks.inspectit.ocelot.config.loaders;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to load the default and fallback configs present in the resource folder.
 */
@UtilityClass
public class ConfigFileLoader {
    /**
     * The encoding used to decode the content of the loaded files.
     */
    static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * This String resembles the classpaths that are searched to get the default config files.
     */
    static final String DEFAULT_CLASSPATH = "classpath:rocks/inspectit/ocelot/config/default/**/*.yml";

    /**
     * This String resembles the classpaths that are searched to get the fallback config files.
     */
    static final String FALLBACK_CLASSPATH = "classpath:rocks/inspectit/ocelot/config/fallback/**/*.yml";

    private String commonPrefix = null;

    /**
     * Returns all files found in the default config path as a key value pair consisting of the path to the file and
     * it's content.
     *
     * @return A Map consisting of the paths and the values of all files.
     */
    public Map<String, String> getDefaultConfigFiles() throws IOException {
        return loadConfig(DEFAULT_CLASSPATH);
    }

    /**
     * Returns all files found in the fallback config path as a key value pair consisting of the path to the file and
     * it's content.
     *
     * @return A Map consisting of the paths and the values of all files.
     */
    public Map<String, String> getFallbackConfigFiles() throws IOException {
        return loadConfig(FALLBACK_CLASSPATH);
    }

    /**
     * Returns all files found in the default config path as an array of resource objects.
     *
     * @return An array containing all default config files as resource objects.
     */
    public Resource[] getDefaultResources() throws IOException {
        return getRessources(DEFAULT_CLASSPATH);
    }

    /**
     * Returns all files found in the fallback config path as an array of resource objects.
     *
     * @return An array containing all fallback config files as resource objects.
     */
    public Resource[] getFallBackResources() throws IOException {
        return getRessources(DEFAULT_CLASSPATH);
    }

    /**
     * This method loads all default config files present in the resource directory of the config project.
     * The files are returned in a map. The keys are the path of the file, and the values are the the file's content.
     * The path to the file is cleaned. The whole section leading to the /default folder is removed.
     *
     * @return A Map containing pairs of file paths and contents, both as String.
     */
    private HashMap<String, String> loadConfig(String path) throws IOException {
        HashMap<String, String> configMap = new HashMap<>();
        Resource[] resources = getRessources(path);
        for (Resource resource : resources) {
            configMap.put(getPathOfResource(resource, resources), readResourceContent(resource));
        }
        return configMap;
    }

    /**
     * This method takes a resource instance as parameter and returns it's content.
     *
     * @param resource The resource instance the content should be returned from.
     * @return The content of the resource.
     */
    private String readResourceContent(Resource resource) throws IOException {
        return IOUtils.toString(resource.getInputStream(), ENCODING);
    }

    /**
     * Takes a path as parameter and returns the resource found in the path.
     *
     * @param path the path to the resource that should be loaded.
     * @return the loaded resource.
     */
    private Resource[] getRessources(String path) throws IOException {
        return new PathMatchingResourcePatternResolver(ConfigFileLoader.class.getClassLoader()).getResources(path);

    }

    /**
     * Returns the path of a given resource instance.
     *
     * @param resource The resource the path should be returned of.
     * @return The path of the given resource.
     */
    private String getPathOfResource(Resource resource, Resource[] resources) {
        String description = resource.getDescription();
        description.replace(File.separator, "/");
        if (commonPrefix == null || !resource.getDescription().startsWith(commonPrefix)) {
            setCommonPrefix(resources);
        }
        description = description.replace(commonPrefix, "");
        return description.substring(0, description.length() - 1);
    }

    /**
     * Sets the current common prefix of all resource files.
     *
     * @param resources
     */
    private void setCommonPrefix(Resource[] resources) {
        List<String> descriptors = new ArrayList<>();
        for (Resource resource : resources) {
            descriptors.add(resource.getDescription());
        }
        commonPrefix = StringUtils.getCommonPrefix(descriptors.stream().toArray(String[]::new));
    }
}
