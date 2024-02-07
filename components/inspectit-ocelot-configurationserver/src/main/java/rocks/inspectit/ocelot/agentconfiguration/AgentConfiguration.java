package rocks.inspectit.ocelot.agentconfiguration;

import com.google.common.annotations.VisibleForTesting;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An {@link AgentMapping} which has its configuration loaded in-memory.
 * In addition, a cryptographic hash is computed to detect changes of configurations.
 */
@Value
@Slf4j
public class AgentConfiguration {

    /**
     * Used as maker in {@link #attributesToConfigurationCache} to mark attribute-maps for which no mapping matches.
     */
    public static AgentConfiguration NO_MATCHING_MAPPING = createDefault();

    /**
     * Predicate to check if a given file path ends with .yml or .yaml
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase()
            .endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    /**
     * The agent mapping for which this instance represents the loaded configuration.
     */
    private AgentMapping mapping;

    /**
     * The list of suppliers for documentable objects. <br>
     * There is a setset of defined documentable objects in this configuration for each file.
     */
    private static Set<AgentDocumentationSupplier> docsSuppliers = new HashSet<>();

    /**
     * The merged YAML configuration for the given mapping.
     */
    private String configYaml;

    /**
     * Cryptographic hash for {@link #configYaml}.
     */
    private String hash;

    private AgentConfiguration(AgentMapping mapping, String configYaml, String hash) {
        this.mapping = mapping;
        this.configYaml = configYaml;
        this.hash = hash;
    }

    private AgentConfiguration(AgentMapping mapping, Set<AgentDocumentationSupplier> agentDocsSuppliers, String configYaml, String hash) {
        this.mapping = mapping;
        docsSuppliers = agentDocsSuppliers;
        this.configYaml = configYaml;
        this.hash = hash;
    }

    /**
     * Factory method to create AgentConfigurations.
     * Also creates a cryptographic hash.
     *
     * @param mapping The agent mapping for which this instance represents the loaded configuration
     * @return Created AgentConfiguration
     */
    public static AgentConfiguration create(AgentMapping mapping, AbstractFileAccessor fileAccessor) {
        String configYaml = loadConfigForMapping(mapping, fileAccessor);
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(mapping, configYaml, hash);
    }

    /**
     * Factory method to create AgentConfigurations.
     * Also creates a cryptographic hash.
     *
     * @param mapping The agent mapping for which this instance represents the loaded configuration
     * @param docsSuppliers The set of
     * @param configYaml The yaml string, which contains the configuration
     * @return Created AgentConfiguration
     */
    public static AgentConfiguration create(AgentMapping mapping, Set<AgentDocumentationSupplier> docsSuppliers, String configYaml) {
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(mapping, docsSuppliers, configYaml, hash);
    }

    /**
     * Factory method to create default AgentConfiguration.
     *
     * @return Created default AgentConfiguration
     */
    public static AgentConfiguration createDefault() {
        String configYaml = "";
        String hash = DigestUtils.md5DigestAsHex(configYaml.getBytes(Charset.defaultCharset()));
        return new AgentConfiguration(null, new HashSet<>(), configYaml, hash);
    }

    /**
     * Convert the set of agent documentation suppliers to a map
     * @return the sets of documentable objects for each file
     */
    public Map<String, Set<String>> getDocsObjectsAsMap() {
        return docsSuppliers.stream()
                .collect(Collectors.toMap(
                        docsSupplier -> docsSupplier.get().filePath(),
                        docsSupplier -> docsSupplier.get().objects()
                ));
    }

    /**
     * Loads the given mapping as yaml string.
     *
     * @param mapping the mapping to load
     *
     * @return the merged yaml for the given mapping or an empty string if the mapping does not contain any existing files
     * If this task has been canceled, null is returned.
     */
    @VisibleForTesting
    static String loadConfigForMapping(AgentMapping mapping, AbstractFileAccessor fileAccessor) {
        LinkedHashSet<String> allYamlFiles = getAllYamlFilesForMapping(fileAccessor, mapping);

        Object result = null;
        for (String path : allYamlFiles) {
            String src = fileAccessor.readConfigurationFile(path).orElse("");
            result = ObjectStructureMerger.loadAndMergeYaml(src, result, path);

            AgentDocumentationSupplier docsSupplier = new AgentDocumentationSupplier(() -> loadDocsObjects(src, path));
            docsSuppliers.add(docsSupplier);
        }
        return result == null ? "" : new Yaml().dump(result);
    }

    /**
     * Returns the set of yaml files, which is defined in the agent mapping sources.
     *
     * @param fileAccessor the accessor to use for reading the file
     * @param mapping the mapping, which contains a list of source file paths
     *
     * @return the set of yaml file paths for the provided mapping
     * If this task has been canceled, null is returned.
     */
    private static LinkedHashSet<String> getAllYamlFilesForMapping(AbstractFileAccessor fileAccessor, AgentMapping mapping) {
        LinkedHashSet<String> allYamlFiles = new LinkedHashSet<>();
        for (String path : mapping.sources()) {
            List<String> yamlFiles = getAllYamlFiles(fileAccessor, path);
            allYamlFiles.addAll(yamlFiles);
        }
        return allYamlFiles;
    }

    /**
     * If the given path is a yaml file, a list containing only it is returned.
     * If the path is a directory, the absolute path of all contained yaml files is returned in alphabetical order.
     * If it is neither, an empty list is returned.
     *
     * @param path the path to check for yaml files, can start with a slash which will be ignored
     *
     * @return a list of absolute paths of contained YAML files
     */
    private static List<String> getAllYamlFiles(AbstractFileAccessor fileAccessor, String path) {
        String cleanedPath;
        if (path.startsWith("/")) {
            cleanedPath = path.substring(1);
        } else {
            cleanedPath = path;
        }

        if (fileAccessor.configurationFileExists(cleanedPath)) {
            if (fileAccessor.configurationFileIsDirectory(cleanedPath)) {
                List<FileInfo> fileInfos = fileAccessor.listConfigurationFiles(cleanedPath);

                return fileInfos.stream()
                        .flatMap(file -> file.getAbsoluteFilePaths(cleanedPath))
                        .filter(HAS_YAML_ENDING)
                        .sorted()
                        .collect(Collectors.toList());
            } else if (HAS_YAML_ENDING.test(cleanedPath)) {
                return Collections.singletonList(cleanedPath);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Loads all documentable objects of the yaml source string.
     *
     * @param src the yaml string
     * @param filePath the path to the yaml file
     *
     * @return the set of documentable objects
     */
    private static AgentDocumentation loadDocsObjects(String src, String filePath) {
        Set<String> objects = Collections.emptySet();
        try {
            objects = DocsObjectsLoader.loadObjects(src);
        } catch (Exception e) {
            log.warn("Could not parse configuration: {}", filePath, e);
        }
        return new AgentDocumentation(filePath, objects);
    }
}
