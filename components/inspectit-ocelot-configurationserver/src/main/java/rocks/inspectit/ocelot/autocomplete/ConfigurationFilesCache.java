package rocks.inspectit.ocelot.autocomplete;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.config.loaders.ConfigFileLoader;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConfigurationFilesCache {

    /**
     * Predicate to check if a given file path ends with .yml or .yaml.
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    @Autowired
    private FileManager fileManager;

    private Collection<Object> yamlContents;

    private HashMap<String, String> fileContents;

    /**
     * Returns the most recently loaded .yaml and .yml files as a list of Objects. Each Object resembles the corresponding
     * files root element. All following elements are then appended to this root element.
     * The objects are either nested Lists or Maps.
     * e.g.: the file x.yaml is loaded with the content
     * root:<br>
     * listOne:<br>
     * - valueOne<br>
     * - valueTwo<br>
     * setOne:<br>
     * valueThree
     * In the returned list there would be the Map "root" containing a key "root", which contains Map containing
     * a key "listOne" with a list containing the elements "valueOne" and "valueTwo". Element "setOne" would only contain
     * the value "valueThree".
     *
     * @return A Collection containing all loaded .yaml and .yml files root elements as Maps or Lists.
     */
    public Collection<Object> getParsedConfigurationFiles() {
        return yamlContents;
    }

    /**
     * Returns the most recently loaded files as key value pairs resembling the path to the file and its contents.
     *
     * @return A HashMap in which all keys resemble a path to a file. The respective contents resemble the files
     * content.
     */
    public HashMap<String, String> getFiles() {
        return fileContents;
    }

    /**
     * Loads all .yaml and .yml files. The files are loaded from the "configuration" folder of the server and from the
     * "files" folder of the working directory. The files contents are parsed into either nested Lists or Maps.
     */
    @PostConstruct
    @EventListener(FileChangedEvent.class)
    public void loadFiles() throws IOException {
        loadFileContents();
        loadYamlContents();
    }

    /**
     * Loads all .yaml and .yml files and saves them as instances of the Yaml class. Before this method is executed,
     * loadFileContents should be executed at least once.
     */
    private void loadYamlContents() {
        yamlContents = fileContents.keySet().stream().filter(HAS_YAML_ENDING)
                .map(key -> parseStringToYamlObject(fileContents.get(key)))
                .filter(o -> o instanceof Map || o instanceof List)
                .collect(Collectors.toList());
    }

    /**
     * Loads all files and saves them as key value pairs consisting of the files path and its contents.
     */
    private void loadFileContents() throws IOException {
        fileContents = loadFilesAsMap(getAllPaths().stream()
                .filter(path -> !path.startsWith(".git"))
                .collect(Collectors.toList()));
        fileContents.putAll(ConfigFileLoader.getDefaultConfigFiles());
    }

    /**
     * Takes as String and parses it either into a nested List or Maps.
     * Literals such as "name:" are parsed as keys for Maps. The respectively following literals are then added as values to this
     * key.
     * Literals such as "- a \n - b" are parsed as lists.
     * All other literals are parsed as scalars and are added as values.
     *
     * @param content The String to be parsed.
     * @return The String parsed into a nested Lists or Map.
     */
    private Object parseStringToYamlObject(String content) {
        Yaml yaml = new Yaml();
        return yaml.load(content);
    }

    /**
     * Searches in the current directory for files with .yml or .yaml ending. Returns all paths to those files as a
     * lexicographically ordered List of Strings.
     *
     * @return A list of all found paths to .yml or .yaml files.
     */
    @VisibleForTesting
    List<String> getAllPaths() {
        try {
            return fileManager.getFilesInDirectory("", true).stream()
                    .flatMap(f -> f.getAbsoluteFilePaths(""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Takes a list of paths to files as strings as attribute. Loads these files and puts the path and the files content
     * as key value pair into a HashMap and returns it. Removes all "\r" escape sequences from the files contents.
     *
     * @param paths A list of paths to files which should be loaded.
     * @return The paths and the contents of the corresponding files as key value pair in a HashMap.
     */
    private HashMap<String, String> loadFilesAsMap(List<String> paths) {
        HashMap<String, String> map = new HashMap<>();
        for (String path : paths) {
            String content = loadContent(path).replace("\r", "");
            if (!content.equals("")) {
                map.put(path, content);
            }
        }
        return map;
    }

    /**
     * Takes a String resembling a path and returns the content of the file found under the given path. Returns an empty
     * String if the file does not exist or an error occurs during loading.
     *
     * @param path The path to the file which should be loaded.
     * @return The content of the file found under the given path.
     */
    private String loadContent(String path) {
        String content = "";
        try {
            content = fileManager.readFile(path);
        } catch (IOException e) {
            log.warn("Unable to load file with path {}", path);
        }
        return content;
    }
}
