package rocks.inspectit.ocelot.core.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import rocks.inspectit.ocelot.config.loaders.ConfigFileLoader;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.config.ConfigSettings;
import rocks.inspectit.ocelot.config.utils.CaseUtils;
import rocks.inspectit.ocelot.core.config.propertysources.EnvironmentInformationPropertySource;
import rocks.inspectit.ocelot.core.config.propertysources.file.DirectoryPropertySource;
import rocks.inspectit.ocelot.core.config.propertysources.http.HttpPropertySourceState;
import rocks.inspectit.ocelot.core.config.util.PropertyUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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

    /**
     * The prefix of all inspectIT related configuration properties.
     */
    private static final String INSPECTIT_ROOT_PREFIX = "inspectit";

    /**
     * The prefix of the settings for all configuration sources ({@link ConfigSettings}).
     */
    private static final String INSPECTIT_CONFIG_SETTINGS_PREFIX = "inspectit.config";

    /**
     * The name to use for the property source holding the {@link rocks.inspectit.ocelot.config.model.env.EnvironmentSettings},
     * Used for the {@link EnvironmentInformationPropertySource}.
     */
    private static final String INSPECTIT_ENV_PROPERTYSOURCE_NAME = "inspectitEnvironment";

    /**
     * The name to use for the proeprty source containing the default configuration overrides.
     */
    public static final String DEFAULT_CONFIG_PROPERTYSOURCE_NAME = "inspectitDefaults";

    /**
     * The name to use for the proeprty source containing the fallback configuration overrides.
     */
    private static final String FALLBACK_CONFIG_PROPERTYSOURCE_NAME = "inspectitFallbackOverwrites";

    /**
     * The name to use for the HTTP configuration source.
     */
    public static final String HTTP_BASED_CONFIGURATION = "httpBasedConfig";

    /**
     * The name of the configuration source to use for a JSON passed in via agent arguments.
     */
    private static final String CMD_ARGS_PROPERTYSOURCE_NAME = "javaagentArguments";

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
            InspectitEnvironment::addFileBasedConfiguration,
            InspectitEnvironment::addHttpBasedConfiguration
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
     * @param ctx         the context to apply this environment onto
     * @param cmdLineArgs the command line arguments, which gets interpreted as JSON configuration
     */
    public InspectitEnvironment(ConfigurableApplicationContext ctx, Optional<String> cmdLineArgs) {
        try {
            configurePropertySources(cmdLineArgs);
        } catch (IOException e) {
            log.error("Error during setup of inspectit environment: " + e.getMessage());
        }

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
        eventDrain.publishEvent(new PropertySourcesChangedEvent(this));
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
     * Initialization of all configuration sources
     * We do not use {@link #customizePropertySources(MutablePropertySources)}, because there it is not possible to access the command line arguments
     */
    protected void configurePropertySources(Optional<String> cmdLineArgs) throws IOException {
        val propsList = getPropertySources();

        loadCmdLineArgumentsPropertySource(cmdLineArgs, propsList);

        PropertySource defaultSettings = loadAgentResourceYaml(DEFAULT_CONFIG_PROPERTYSOURCE_NAME, ConfigFileLoader.getDefaultResources());
        propsList.addLast(defaultSettings);
        propsList.addLast(new EnvironmentInformationPropertySource(INSPECTIT_ENV_PROPERTYSOURCE_NAME));

        Optional<ConfigSettings> appliedConfigSettings = initializeConfigurationSources(propsList);

        log.info("Registered Configuration Sources:");
        getPropertySources().stream().forEach(ps -> log.info("  {}", ps.getName()));

        Optional<InspectitConfig> initialConfig = loadAndValidateFromProperties(INSPECTIT_ROOT_PREFIX, InspectitConfig.class);
        if (initialConfig.isPresent()) {
            currentConfig = initialConfig.get();
        } else {
            log.error("Startup configuration is not valid! Using fallback configuration but listening for configuration updates...");
            PropertySource fallbackSettings = loadAgentResourceYaml(FALLBACK_CONFIG_PROPERTYSOURCE_NAME, ConfigFileLoader.getFallBackResources());
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

    private void loadCmdLineArgumentsPropertySource(Optional<String> cmdLineArgs, MutablePropertySources propsList) {
        if (cmdLineArgs.isPresent() && !cmdLineArgs.get().isEmpty()) {
            try {
                Properties config = PropertyUtils.readJson(cmdLineArgs.get());
                PropertiesPropertySource pps = new PropertiesPropertySource(CMD_ARGS_PROPERTYSOURCE_NAME, config);
                propsList.addFirst(pps);
            } catch (Exception e) {
                log.error("Could not load javaagent arguments as configuration JSON", e);
            }
        }
    }

    /**
     * Loads and validates a given bean from the environments properties.
     *
     * @param prefix      the prefix to use, e.g. "inspectit" for the root config object
     * @param configClazz the class of the config to load
     * @param <T>         the type of configClazz
     * @return the loaded object in case of success or an empty optional otherwise
     */
    public synchronized <T> Optional<T> loadAndValidateFromProperties(String prefix, Class<T> configClazz) {
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
                String property = CaseUtils.camelCaseToKebabCase(vio.getPropertyPath().toString());
                if (vio.getInvalidValue() instanceof CharSequence
                        || vio.getInvalidValue() instanceof Number
                        || vio.getInvalidValue() instanceof Duration) {
                    log.error("{} (={}) => {}", property, vio.getInvalidValue(), vio.getMessage());
                } else {
                    log.error("{} => {}", property, vio.getMessage());
                }
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

    private static PropertiesPropertySource loadAgentResourceYaml(String propertySourceName, Resource[] resources) {
        Properties result = new Properties();
        for (val res : resources) {
            Properties properties = PropertyUtils.readYamlFiles(res);
            result.putAll(properties);
        }
        return new PropertiesPropertySource(propertySourceName, result);
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

    private static void addHttpBasedConfiguration(MutablePropertySources propsList, ConfigSettings currentConfig) {
        URL url = currentConfig.getHttp().getUrl();
        boolean httpEnabled = currentConfig.getHttp().isEnabled() && url != null;

        if (httpEnabled) {

            HttpPropertySourceState httpSourceState = new HttpPropertySourceState(HTTP_BASED_CONFIGURATION, currentConfig.getHttp());
            try {
                log.info("Initializing HTTP based configuration from URL: {}", httpSourceState.getEffectiveRequestUri());
            } catch (URISyntaxException e) {
                log.error("The syntax of the URL of the HTTP based configuration is not valid", e);
            }
            httpSourceState.update(true);
            propsList.addBefore(InspectitEnvironment.DEFAULT_CONFIG_PROPERTYSOURCE_NAME, httpSourceState.getCurrentPropertySource());
        }
    }
}
