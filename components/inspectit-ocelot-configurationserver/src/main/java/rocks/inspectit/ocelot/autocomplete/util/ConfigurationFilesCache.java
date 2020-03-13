package rocks.inspectit.ocelot.autocomplete.util;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Loads all .yaml and .yml files. The files are loaded from the "configuration" folder of the server and from the
     * "files" folder of the working directory. The files contents are parsed into either nested Lists or Maps.
     */
    @PostConstruct
    @EventListener(FileChangedEvent.class)
    public void loadFiles() throws IOException {
        List<String> filePaths = getAllPaths();
        yamlContents = Stream.concat(
                filePaths.stream()
                        .map(this::loadYamlFile)
                        .filter(Objects::nonNull),
                ConfigFileLoader.getDefaultConfigFiles().values().stream()
                        .map(this::parseYaml)
        ).collect(Collectors.toList());
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
    private Object parseYaml(String content) {
        Yaml yaml = new Yaml();
        return yaml.load(content);
    }

    /**
     * This method loads a .yaml or .yml file found in a given path and returns it either as nested List or Map.
     * The Map/List can either contain a terminal value such as Strings, Lists of elements or Maps. The latter two
     * of which can each again contain Lists, Maps or terminal values as values.
     *
     * @param path path of the file which should be loaded.
     * @return the file as an Object parsed as described above.
     */
    @VisibleForTesting
    Object loadYamlFile(String path) {
        String src;
        try {
            src = fileManager.readFile(path);
        } catch (IOException e) {
            log.warn("Unable to load file with path {}", path);
            return null;
        }
        if (src != null) {
            return parseYaml(src);
        }
        return null;
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
                    .filter(HAS_YAML_ENDING)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
