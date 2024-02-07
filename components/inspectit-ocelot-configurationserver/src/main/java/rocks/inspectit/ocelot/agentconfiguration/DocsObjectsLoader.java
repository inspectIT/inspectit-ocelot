package rocks.inspectit.ocelot.agentconfiguration;

import inspectit.ocelot.configdocsgenerator.parsing.ConfigParser;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;

import java.io.IOException;
import java.util.*;

/**
 * Helper class to load documentable objects from a yaml string
 */
@Slf4j
public class DocsObjectsLoader {

    /**
     * Use the same constant as the ui in 'src/data/constants.js'
     */
    public final static String OCELOT_DEFAULT_CONFIG_PREFIX = "/$%$%$%$%Ocelot-default-key/";

    /**
     * Loads all documentable objects, like actions, scopes, rules & metrics from the provided inspectIT yaml
     * @param src the source yaml
     * @return a set of defined objects in this yaml
     */
    public static Set<String> loadObjects(String src) throws IOException {
        Yaml yaml = new Yaml();
        Object rawYaml = yaml.load(src);
        Set<String> objects = new HashSet<>();

        if(rawYaml != null) {
            String cleanYaml = yaml.dump(rawYaml);
            ConfigParser configParser = new ConfigParser();
                InspectitConfig config = configParser.parseConfig(cleanYaml);
                InstrumentationSettings instrumentation = config.getInstrumentation();

                if(instrumentation != null) {
                    instrumentation.getActions().forEach((name, action) -> objects.add(name));
                    instrumentation.getScopes().forEach((name, scope) -> objects.add(name));
                    instrumentation.getRules().forEach((name, rule) -> objects.add(name));
                }
                config.getMetrics().getDefinitions().forEach((name, metric) -> objects.add(name));
        }

        return objects;
    }

    /**
     * Loads all documentable objects of each file for the current agent
     * @param defaultYamls A map of the default file paths and their yaml content
     * @return A set of defined objects for each file
     */
    public static Map<String, Set<String>> loadDefaultDocsObjectsByFile(Map<String, String> defaultYamls) {
        Map<String, Set<String>> defaultDocsObjectsByFile = new HashMap<>();
        for(Map.Entry<String, String> entry : defaultYamls.entrySet()) {
            String path = OCELOT_DEFAULT_CONFIG_PREFIX + entry.getKey();
            String src = entry.getValue();
            Set<String> objects = Collections.emptySet();

            try {
                objects = loadObjects(src);
            } catch (Exception e) {
                log.warn("Could not parse configuration: {}", path, e);
            }
            defaultDocsObjectsByFile.put(path, objects);
        }

        return defaultDocsObjectsByFile;
    }
}
