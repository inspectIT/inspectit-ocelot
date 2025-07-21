package rocks.inspectit.ocelot.config.loaders;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
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
    static final String DEFAULT_CLASSPATH = "classpath:shadow/rocks/inspectit/ocelot/config/default/**/*.yml";

    /**
     * This String resembles the classpaths that are searched to get the fallback config files.
     */
    static final String FALLBACK_CLASSPATH = "classpath:shadow/rocks/inspectit/ocelot/config/fallback/**/*.yml";

    /**
     * Returns all files found in the default config path as a key value pair consisting of the path to the file and
     * its content.
     *
     * @return A Map consisting of the paths and the values of all files.
     */
    public Map<String, String> getDefaultConfigFiles() throws IOException {
        return loadConfig(getDefaultResources());
    }

    /**
     * Returns all files found in the fallback config path as a key value pair consisting of the path to the file and
     * its content.
     *
     * @return A Map consisting of the paths and the values of all files.
     */
    public Map<String, String> getFallbackConfigFiles() throws IOException {
        return loadConfig(getFallBackResources());
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
        return getRessources(FALLBACK_CLASSPATH);
    }

    /**
     * This method loads all config files present in the resource directory of the config project.
     * The files are returned in a map. The keys are the path of the file, and the values are the the file's content.
     * The path to the file is cleaned by removing the substring leading to the /default folder.
     *
     * @return A Map containing pairs of file paths and contents, both as String.
     */
    private HashMap<String, String> loadConfig(Resource[] resources) throws IOException {
        HashMap<String, String> configMap = new HashMap<>();
        String commonPrefix = StringUtils.getCommonPrefix(
                Arrays.stream(resources)
                        .map(ConfigFileLoader::getResourcePath)
                        .toArray(String[]::new)
        );
        for (Resource resource : resources) {
            String resourceDescription = getResourcePath(resource)
                    .substring(commonPrefix.length())
                    .replace(File.separator, "/");
            configMap.put(resourceDescription, readResourceContent(resource));
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
     * Return the full path of a given resource object.
     *
     * @param resource The resource object the path should be returned of.
     * @return The full path of the resource. e.g. "C:/path/to/my/resource/file"
     */
    private static String getResourcePath(Resource resource) {
        if (resource instanceof ClassPathResource) {
            return ((ClassPathResource) resource).getPath();
        } else {
            try {
                return resource.getFile().getPath();
            } catch (IOException e) {
                throw new RuntimeException("Cannot resolve resource path because it is neither a file nor a class path resource", e);
            }
        }
    }
}
