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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YamlLoader {

    /**
     * Predicate to check if a given file path ends with .yml or .yaml.
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    @Autowired
    private FileManager fileManager;

    private Collection<Object> yamlContents;

    public Collection<Object> getYamlContents() {
        return yamlContents;
    }

    @PostConstruct
    @EventListener(FileChangedEvent.class)
    public void loadFiles() throws IOException {
        yamlContents = getAllPaths().stream()
                .map(this::loadYaml)
                .filter(o -> o instanceof Map || o instanceof List)
                .flatMap(a -> toCollection(a).stream())
                .collect(Collectors.toList());
        yamlContents.addAll(ConfigFileLoader.getDefaultConfigFiles().values());
    }

    /**
     * This method loads a yaml or .yml file in any given path.
     *
     * @param path path of the yaml to load.
     * @return the file as Object.
     */
    @VisibleForTesting
    Object loadYaml(String path) {
        Yaml yaml = getYaml();
        String src;
        try {
            src = fileManager.readFile(path);
        } catch (IOException e) {
            log.warn("Unable to load file with path {}", path);
            return null;
        }
        if (src != null) {
            return yaml.load(src);
        }
        return null;
    }

    Yaml getYaml() {
        return new Yaml();
    }

    /**
     * Searches in the current directory for files with .yml or .yaml ending. Returns all paths to those files as
     * List of the type string.
     *
     * @return A list of all found paths to .yml or .yaml files.
     */
    @VisibleForTesting
    List<String> getAllPaths() {
        try {
            return fileManager.getFilesInDirectory(null, true).stream()
                    .flatMap(f -> f.getAbsoluteFilePaths(""))
                    .filter(HAS_YAML_ENDING)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Returns an object as collection.
     * If the object is an instance of List, the List itself is returned.
     * If the object is an instance of Map, the values of the map are returned as a collection.
     * If the object is neither an instance of Map nor an instance of list, an empty list is returned.
     *
     * @param o The object the collection should be returned of.
     * @return A collection containing the objects contents.
     */
    private Collection<?> toCollection(Object o) {
        if (o instanceof List) {
            return (List) o;
        }
        if (o instanceof Map) {
            return ((Map) o).values();
        }
        return Collections.emptyList();
    }
}
