package rocks.inspectit.ocelot.autocomplete.util;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.config.loaders.ConfigFileLoader;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.utils.CancellableTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Loads all configuration files of the given revision and parses them.
 */
@Slf4j
public class ConfigurationFilesCacheReloadTask extends CancellableTask<Collection<Object>> {

    /**
     * Predicate to check if a given file path ends with .yml or .yaml.
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase()
            .endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    private AbstractFileAccessor fileAccess;

    public ConfigurationFilesCacheReloadTask(AbstractFileAccessor fileAccess, Consumer<Collection<Object>> onLoadCallback) {
        super(onLoadCallback);
        this.fileAccess = fileAccess;
    }

    @Override
    public void run() {
        try {
            List<Object> parsedFileContents = new ArrayList<>();
            List<String> filePaths = getAllPaths();
            for (String path : filePaths) {
                if (isCanceled()) {
                    return;
                }
                Object parsed = loadYamlFile(path);
                if (parsed != null) {
                    parsedFileContents.add(parsed);
                }
            }
            ConfigFileLoader.getDefaultConfigFiles()
                    .values()
                    .stream()
                    .map(this::parseYaml)
                    .forEach(parsedFileContents::add);
            onTaskSuccess(parsedFileContents);
        } catch (Exception e) {
            log.error("Error refreshing cache for autocompleter", e);
        }

    }

    /**
     * Takes as String and parses it either into a nested List or Maps.
     * Literals such as "name:" are parsed as keys for Maps. The respectively following literals are then added as values to this
     * key.
     * Literals such as "- a \n - b" are parsed as lists.
     * All other literals are parsed as scalars and are added as values.
     *
     * @param content The String to be parsed.
     *
     * @return The String parsed into a nested Lists or Map.
     */
    @VisibleForTesting
    Object parseYaml(String content) {
        Yaml yaml = new Yaml();
        return yaml.load(content);
    }

    /**
     * This method loads a .yaml or .yml file found in a given path and returns it either as nested List or Map.
     * The Map/List can either contain a terminal value such as Strings, Lists of elements or Maps. The latter two
     * of which can each again contain Lists, Maps or terminal values as values.
     *
     * @param path path of the file which should be loaded.
     *
     * @return the file as an Object parsed as described above.
     */
    @VisibleForTesting
    Object loadYamlFile(String path) {
        try {
            Optional<String> src = fileAccess.readConfigurationFile(path);
            return src.map(this::parseYaml).orElse(null);
        } catch (Exception e) {
            log.warn("Unable to load file with path {}", path);
            return null;
        }
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
            List<FileInfo> fileInfos = fileAccess.listConfigurationFiles("");

            return fileInfos.stream()
                    .flatMap(file -> file.getAbsoluteFilePaths(""))
                    .filter(HAS_YAML_ENDING)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
