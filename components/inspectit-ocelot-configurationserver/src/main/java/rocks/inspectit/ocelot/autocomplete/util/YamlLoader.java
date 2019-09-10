package rocks.inspectit.ocelot.autocomplete.util;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileManager;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YamlLoader {

    /**
     * Predicate to check if a given file path ends with .yml or .yaml
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");
    private static final String DEFAULT_CONFIG_PATH = "classpath:rocks/inspectit/ocelot/config/default/**/*.yml";

    @Autowired
    private FileManager fileManager;

    private Collection<Object> yamlContents;

    public Collection<Object> getYamlContents() {
        return yamlContents;
    }

    @PostConstruct
    @EventListener(FileChangedEvent.class)
    public void loadFiles() {
        yamlContents = getAllPaths().stream()
                .map(path -> loadYaml(path))
                .filter(o -> o instanceof Map || o instanceof List)
                .collect(Collectors.toList());
        yamlContents.addAll(loadDefaultConfig().stream()
                .filter(o -> o instanceof Map || o instanceof List)
                .collect(Collectors.toList()));
    }

    /**
     * This method lodas a yaml or .yml file in any given path
     *
     * @param path path of the yaml to load
     * @return the file as Object
     */
    Object loadYaml(String path) {
        Yaml yaml = new Yaml();
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

    /**
     * Loads the default config provided in the Project and returns it as a List of objects.
     *
     * @return
     */
    public List<Object> loadDefaultConfig() {
        Yaml yaml = new Yaml();
        ArrayList<Object> yamls = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver(YamlLoader.class.getClassLoader())
                    .getResources(DEFAULT_CONFIG_PATH);
            for (val res : resources) {
                try {
                    yamls.add(yaml.load(res.getInputStream()));
                } catch (IOException e) {
                    log.error("Error trying to read a HashMap from default settings");
                }
            }
        } catch (IOException e) {
            log.error("Error trying to read default settings");
        }

        return yamls;

    }

    /**
     * Searches in the current directory for files with .yml or .yaml ending. Returns all paths to those files as
     * List of the type string
     *
     * @return A list of all found paths to .yml or .yaml files
     */
    private List<String> getAllPaths() {
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
}
