package rocks.inspectit.oce.core.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import rocks.inspectit.oce.core.config.filebased.DirectoryPropertySource;
import rocks.inspectit.oce.core.config.filebased.PropertyFileUtils;
import rocks.inspectit.oce.core.config.model.InspectitConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * InspectIT Spring Environment.
 * This Environment extends the {@link StandardEnvironment} by additional property sources such as {@link DirectoryPropertySource}.
 * In addition this class offers a thread-safe way of reading and altering the configuration through {@link #readPropertySources(Consumer)} and {@link #updatePropertySources(Consumer)}.
 * Configuration changes are published by firing {@link InspectitConfigChangedEvent}s.
 */
@Slf4j
public class InspectitEnvironment extends StandardEnvironment {

    private static final String DEFAULT_CONFIG_PATH = "/config/default.yml";
    private static final String DEFAULT_CONFIG_PROPERTYSOURCE_NAME = "inspectitDefaults";


    /**
     * Sorted list of all configuration sources.
     * They are loaded in the given order. The earlier a configuration appears in this list, the higher its priority.
     * This means that configurations loaded from items appearing earlier in the list overwrite configurations from items appearing later in the list.
     * In contrast items appearing first in the list can provide information for loading items appearing later in the list.
     */
    private static final List<BiConsumer<MutablePropertySources, InspectitConfig>> configurationInitializationSteps = Arrays.asList(
            InspectitEnvironment::addFileBasedConfiguration
    );

    /**
     * The currently active inspectIT configuration.
     */
    @Getter
    private InspectitConfig currentConfig;

    /**
     * Event drain for publishing {@link InspectitConfigChangedEvent}s.
     */
    private ApplicationEventPublisher eventDrain;

    public InspectitEnvironment(ApplicationEventPublisher eventDrain) {
        this.eventDrain = eventDrain;
    }

    /**
     * Allows a thread-safe access to the property sources of the environment.
     * The property sources may be changed, new sources can be added and existing sources can be removed.
     * After the update has been executed the environment automatically reloads the {@link #currentConfig}
     * and fires a {@link InspectitConfigChangedEvent} if the configuration changed.
     *
     * @param propertiesUpdater the function to execute for updating the properties. It is guaranteed that
     *                          neither the property sources nor {@link #currentConfig} will change during the execution of propertiesUpdater.
     */
    public synchronized void updatePropertySources(Consumer<MutablePropertySources> propertiesUpdater) {
        propertiesUpdater.accept(getPropertySources());
        InspectitConfig oldConfig = currentConfig;
        reloadConfig();
        if (!currentConfig.equals(oldConfig)) {
            val event = new InspectitConfigChangedEvent(this, oldConfig, currentConfig);
            eventDrain.publishEvent(event);
        }
    }

    /**
     * Allows a thread-safe READ-ONLY access to the property sources of the environment.
     *
     * @param propertiesAccessor the function to execute for updating the properties. It is guaranteed that
     *                           neither the property sources nor {@link #currentConfig} will change during the execution of propertiesAccessor.
     */
    public synchronized void readPropertySources(Consumer<PropertySources> propertiesAccessor) {
        propertiesAccessor.accept(getPropertySources());
    }

    private void reloadConfig() {
        currentConfig = Binder.get(this).bind("inspectit", InspectitConfig.class).get();
    }

    @Override
    protected void customizePropertySources(MutablePropertySources propsList) {
        //include the standard sources (e.g. environment variables and properties)
        super.customizePropertySources(propsList);

        addAgentDefaultYaml(propsList);

        reloadConfig();
        for (val initializer : configurationInitializationSteps) {
            initializer.accept(propsList, currentConfig);
            reloadConfig();
        }

        log.info("Registered Configuration Sources:");
        getPropertySources().stream().forEach(ps -> log.info("  {}", ps.getName()));
    }

    private static void addAgentDefaultYaml(MutablePropertySources propsList) {
        ClassPathResource defaultYamlResource = new ClassPathResource(DEFAULT_CONFIG_PATH, InspectitEnvironment.class.getClassLoader());
        Properties defaultProps = PropertyFileUtils.readYamlFiles(defaultYamlResource);
        propsList.addLast(new PropertiesPropertySource(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, defaultProps));
    }

    private static void addFileBasedConfiguration(MutablePropertySources propsList, InspectitConfig currentConfig) {
        String path = currentConfig.getConfig().getFileBased().getPath();
        Path dirPath = Paths.get(path);
        Boolean enabled = currentConfig.getConfig().getFileBased().isEnabled();
        boolean fileBasedConfigEnabled = enabled && path != null && !path.isEmpty();
        if (fileBasedConfigEnabled) {
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                log.info("initializing file based configuration from dir: {}", path);
                val dps = new DirectoryPropertySource("fileBasedConfig", Paths.get(path));
                propsList.addBefore(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, dps);
                dps.reload(propsList);
            } else {
                log.error("The given configuration file directory does not exist: {}", path);
            }
        }
    }
}
