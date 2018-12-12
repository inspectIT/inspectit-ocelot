package rocks.inspectit.oce.core.rocks.inspectit.oce.core.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * This class is responsible for registering all {@link org.springframework.core.env.PropertySource}s required for the initialization of inspectIT.
 *
 * @author Jonas Kunz
 */
public class PropertySourcesInitializer {

    private static final String DEFAULT_CONFIG_PATH = "/config/default.yml";
    private static final String DEFAULT_CONFIG_PROPERTYSOURCE_NAME = "inspectitDefaults";

    /**
     * Configures the {@link org.springframework.core.env.PropertySource}s of the given
     * @param ctx the spring context
     * @param cmdLineArgs the command line args passed to inspectIT
     */
    public static void configurePropertySources(AnnotationConfigApplicationContext ctx, String cmdLineArgs) {
        MutablePropertySources propsList = ctx.getEnvironment().getPropertySources();

        addCommandLineConfigDir(propsList, cmdLineArgs);
        addAgentDefaultYaml(propsList);
    }

    private static void addAgentDefaultYaml(MutablePropertySources propsList) {
        ClassPathResource defaultYamlResource = new ClassPathResource(DEFAULT_CONFIG_PATH, PropertySourcesInitializer.class.getClassLoader());
        Properties defaultProps = ConfigurationUtils.readYamlsAsProperties(defaultYamlResource);
        propsList.addLast(new PropertiesPropertySource(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, defaultProps));
    }

    private static void addCommandLineConfigDir(MutablePropertySources propsList, String cmdLineArgs) {
        if(cmdLineArgs != null && !cmdLineArgs.isEmpty()) {
            Path configDir = Paths.get(cmdLineArgs);
            DirectoryPropertySource.addLast("cmdPassedConfigDir", configDir, propsList);
        }
    }
}
