package rocks.inspectit.oce.core.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.env.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import rocks.inspectit.oce.core.config.filebased.DirectoryPropertySource;
import rocks.inspectit.oce.core.config.filebased.PropertyFileUtils;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.config.ConfigSettings;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * InspectIT Spring Environment.
 * This Environment extends the {@link StandardEnvironment} by additional property sources such as {@link DirectoryPropertySource}.
 * In addition this class offers a thread-safe way of reading and altering the configuration through {@link #readPropertySources(Consumer)} and {@link #updatePropertySources(Consumer)}.
 * Configuration changes are published by firing {@link InspectitConfigChangedEvent}s.
 */
@Slf4j
public class InspectitEnvironment extends StandardEnvironment {

    private static final String INSPECTIT_ROOT_PREFIX = "inspectit";
    private static final String INSPECTIT_CONFIG_SETTINGS_PREFIX = "inspectit.config";

    private static final String DEFAULT_CONFIG_PATH = "/config/default.yml";
    private static final String DEFAULT_CONFIG_PROPERTYSOURCE_NAME = "inspectitDefaults";

    private static final String FALLBACK_CONFIG_PATH = "/config/fallback.yml";
    private static final String FALLBACK_CONFIG_PROPERTYSOURCE_NAME = "inspectitFallbackOverwrites";

    /**
     * The variable under which {@link #currentConfig)} is available in bean expressions, such as @Value annotations
     */
    private static final String INSPECTIT_CONFIG_BEAN_EXPRESSION_VARIABLE = "inspectit";


    /**
     * Sorted list of all configuration sources.
     * They are loaded in the given order. The earlier a configuration appears in this list, the higher its priority.
     * This means that configurations loaded from items appearing earlier in the list overwrite configurations from items appearing later in the list.
     * In contrast items appearing first in the list can provide information for loading items appearing later in the list.
     */
    private static final List<BiConsumer<MutablePropertySources, ConfigSettings>> CONFIGURATION_INIT_STEPS = Arrays.asList(
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
    private final ApplicationEventPublisher eventDrain;

    /**
     * Validator used for validating configurations.
     */
    private Validator validator;

    /**
     * Creates and applies an InspectitEnvironment onto the given context.
     *
     * @param ctx the context to apply this environemnt onto
     */
    public InspectitEnvironment(ConfigurableApplicationContext ctx) {
        eventDrain = ctx;
        ctx.setEnvironment(this);
        ctx.addBeanFactoryPostProcessor(fac -> fac.setBeanExpressionResolver(getBeanExpressionResolver()));
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

        Optional<InspectitConfig> newConfig = loadAndValidateFromProperties(INSPECTIT_ROOT_PREFIX, InspectitConfig.class);
        newConfig.ifPresent(c -> currentConfig = c);
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

    /**
     * Initialization of all configuration source
     *
     * @param propsList
     */
    @Override
    protected void customizePropertySources(MutablePropertySources propsList) {
        //include the standard sources (e.g. environment variables and properties)
        super.customizePropertySources(propsList);

        PropertySource defaultSettings = loadAgentResourceYaml(DEFAULT_CONFIG_PATH, DEFAULT_CONFIG_PROPERTYSOURCE_NAME);
        propsList.addLast(defaultSettings);

        Optional<ConfigSettings> appliedConfigSettings = initializeConfigurationSources(propsList);

        log.info("Registered Configuration Sources:");
        getPropertySources().stream().forEach(ps -> log.info("  {}", ps.getName()));

        Optional<InspectitConfig> initialConfig = loadAndValidateFromProperties(INSPECTIT_ROOT_PREFIX, InspectitConfig.class);
        if (initialConfig.isPresent()) {
            currentConfig = initialConfig.get();
        } else {
            log.error("Startup configuration is not valid! Using fallback configuration but listening for configuration updates...");
            PropertySource fallbackSettings = loadAgentResourceYaml(FALLBACK_CONFIG_PATH, FALLBACK_CONFIG_PROPERTYSOURCE_NAME);
            val currentSources = propsList.stream().collect(Collectors.toList());
            val fallbackSources = Arrays.<PropertySource<?>>asList(fallbackSettings, defaultSettings);

            currentSources.forEach(ps -> propsList.remove(ps.getName()));
            fallbackSources.forEach(ps -> propsList.addLast(ps));

            currentConfig = loadAndValidateFromProperties(INSPECTIT_ROOT_PREFIX, InspectitConfig.class).get();

            fallbackSources.forEach(ps -> propsList.remove(ps.getName()));
            currentSources.forEach(ps -> propsList.addLast(ps));

            appliedConfigSettings.ifPresent(currentConfig::setConfig);
        }
    }

    /**
     * Loads and validates a given bean from the environemnts proeprties.
     *
     * @param prefix      the prefix to use, e.g. "inspectit" for the root config object
     * @param configClazz the class of the config to load
     * @param <T>         the type of configClazz
     * @return the loaded object in case of success or an empty optional otherwise
     */
    private <T> Optional<T> loadAndValidateFromProperties(String prefix, Class<T> configClazz) {
        T newConfig;
        try {
            newConfig = Binder.get(this).bind(prefix, configClazz).get();
        } catch (Exception e) {
            log.error("Error loading the configuration '{}'.", prefix, e);
            return Optional.empty();
        }
        Validator validator = getValidator();
        val violations = validator.validate(newConfig);
        if (violations.isEmpty()) {
            return Optional.of(newConfig);
        } else {
            log.error("Error loading the configuration '{}'.", prefix);
            for (ConstraintViolation<T> vio : violations) {
                log.error("{} (={}) => {}", CaseUtils.camelCaseToKebabCase(vio.getPropertyPath().toString()), vio.getInvalidValue(), vio.getMessage());
            }
            return Optional.empty();
        }
    }

    private Validator getValidator() {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }
        return validator;
    }

    private Optional<ConfigSettings> initializeConfigurationSources(MutablePropertySources propsList) {
        Optional<ConfigSettings> lastConfig = Optional.empty();
        Optional<ConfigSettings> config = loadAndValidateFromProperties(INSPECTIT_CONFIG_SETTINGS_PREFIX, ConfigSettings.class);
        for (val initializer : CONFIGURATION_INIT_STEPS) {
            if (!config.isPresent()) {
                log.error("Error loading {}, aborting scanning for additional configuration sources!", INSPECTIT_CONFIG_SETTINGS_PREFIX);
                break;
            }
            initializer.accept(propsList, config.get());
            lastConfig = config;
            config = loadAndValidateFromProperties(INSPECTIT_CONFIG_SETTINGS_PREFIX, ConfigSettings.class);
        }
        return config.isPresent() ? config : lastConfig;
    }

    private static PropertiesPropertySource loadAgentResourceYaml(String resourcePath, String propertySourceName) {
        ClassPathResource defaultYamlResource = new ClassPathResource(resourcePath, InspectitEnvironment.class.getClassLoader());
        Properties defaultProps = PropertyFileUtils.readYamlFiles(defaultYamlResource);
        return new PropertiesPropertySource(propertySourceName, defaultProps);
    }

    /**
     * @return a bean expression resolver in which the #inspectit variable refers to the currently active configuration
     */
    private BeanExpressionResolver getBeanExpressionResolver() {
        return new StandardBeanExpressionResolver() {
            @Override
            protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
                super.customizeEvaluationContext(evalContext);
                evalContext.setVariable(INSPECTIT_CONFIG_BEAN_EXPRESSION_VARIABLE, getCurrentConfig());
            }
        };
    }


    private static void addFileBasedConfiguration(MutablePropertySources propsList, ConfigSettings currentConfig) {
        String path = currentConfig.getFileBased().getPath();
        Path dirPath = Paths.get(path);
        boolean enabled = currentConfig.getFileBased().isEnabled();
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
