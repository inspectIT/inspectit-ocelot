package rocks.inspectit.oce.core.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import rocks.inspectit.oce.core.config.filebased.DirectoryPropertySource;
import rocks.inspectit.oce.core.config.filebased.PropertyFileUtils;
import rocks.inspectit.oce.core.config.model.InspectitConfig;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * This class is responsible for registering all {@link org.springframework.core.env.PropertySource}s required for the initialization of inspectIT.
 *
 * @author Jonas Kunz
 */
@Slf4j
public class PropertySourcesInitializer {

    private static final String DEFAULT_CONFIG_PATH = "/config/default.yml";
    private static final String DEFAULT_CONFIG_PROPERTYSOURCE_NAME = "inspectitDefaults";

    /**
     * Sorted list of all configuration sources.
     * They are loaded in the given order. The earlier a configuration appears in this list, the higher its priority.
     * This means that configurations loaded from items appearing earlier in the list overwrite configurations from items appearing later in the list.
     * In contrast items appearing first in the list can provide information for loading items appearing later in the list.
     */
    private static final List<BiConsumer<MutablePropertySources, InspectitConfig>> configurationInitializationSteps = Arrays.asList(
            PropertySourcesInitializer::addFileBasedConfiguration
    );

    /**
     * Configures the {@link org.springframework.core.env.PropertySource}s of the given spring context.
     *
     * @param ctx the spring context
     */
    public static void configurePropertySources(AnnotationConfigApplicationContext ctx) {
        ConfigurableEnvironment env = ctx.getEnvironment();
        MutablePropertySources propsList = env.getPropertySources();
        addAgentDefaultYaml(propsList);

        for (val initializer : configurationInitializationSteps) {
            initializer.accept(env.getPropertySources(), InspectitConfig.createFromEnvironment(env));
        }

        log.info("-----Registered Configuration Sources-----");
        env.getPropertySources().stream().forEach(ps -> log.info(ps.getName()));
    }

    private static void addAgentDefaultYaml(MutablePropertySources propsList) {
        ClassPathResource defaultYamlResource = new ClassPathResource(DEFAULT_CONFIG_PATH, PropertySourcesInitializer.class.getClassLoader());
        Properties defaultProps = PropertyFileUtils.readYamlFiles(defaultYamlResource);
        propsList.addLast(new PropertiesPropertySource(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, defaultProps));
    }

    private static void addFileBasedConfiguration(MutablePropertySources propsList, InspectitConfig currentConfig) {
        String path = currentConfig.getConfig().getFileBased().getPath();
        Boolean enabled = currentConfig.getConfig().getFileBased().isEnabled();
        boolean fileBasedConfigEnabled = enabled && path != null && !path.isEmpty();
        if (fileBasedConfigEnabled) {
            log.info("initializing file based configuration from dir: {}", path);
            val dps = new DirectoryPropertySource("fileBasedConfig", Paths.get(path));
            propsList.addBefore(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, dps);
            dps.reload(propsList);
        }
    }
}
