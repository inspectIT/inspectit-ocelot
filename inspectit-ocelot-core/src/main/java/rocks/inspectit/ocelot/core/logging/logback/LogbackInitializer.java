package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.logging.LoggingSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Function;

import static rocks.inspectit.ocelot.core.logging.logback.LoggingProperties.*;

/**
 * Main logback initializer. Method {@link #initLogging(InspectitConfig)} can be called
 * whenever inspectIT configuration is changed in order to update the logging settings.
 *
 * @author Ivan Senic
 */
public class LogbackInitializer {

    /**
     * {@link #consoleEnabled} is depending on this function, thus, the field order is important.
     */
    @VisibleForTesting
    static Function<String, String> getEnvironment = System::getenv;

    // flags for the filters
    static boolean consoleEnabled = isConsoleInitiallyEnabled();

    static boolean fileEnabled = isFileInitiallyEnabled();

    /**
     * Initialize logging before reading the {@link InspectitConfig}
     */
    public static void initDefaultLogging() {
        initLogging(null);
    }

    /**
     * (Re-)initializes the logback configuration.
     *
     * @param config inspectIT config to read values from
     */
    public static void initLogging(InspectitConfig config) {
        if (null != config) {
            setPropertiesFromConfig(config);
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            PiccoloSaxEventRecorder recorder = new PiccoloSaxEventRecorder(context);
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            // one input stream cannot be read twice
            recorder.recordEvents(getConfigFileInputStream(config));
            configurator.doConfigure(getConfigFileInputStream(config));
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    @VisibleForTesting
    static boolean isConsoleInitiallyEnabled() {
        return isInitiallyEnabled(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM, INSPECTIT_LOGGING_CONSOLE_ENABLED);
    }

    @VisibleForTesting
    static boolean isFileInitiallyEnabled() {
        return isInitiallyEnabled(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM, INSPECTIT_LOGGING_FILE_ENABLED);
    }

    /**
     * Checks, if a feature is enabled at start up. System properties are higher prioritized than environment variables.
     *
     * @param systemProperty the name of the system property
     * @param envVariable the name of the environment variable
     *
     * @return true, if the property/variable is enabled at start up
     */
    private static boolean isInitiallyEnabled(String systemProperty, String envVariable) {
        String enabledFlag = System.getProperty(systemProperty);
        if (enabledFlag == null) {
            enabledFlag = getEnvironment.apply(envVariable);
        }

        if (enabledFlag == null) {
            return true;
        } else {
            return Boolean.parseBoolean(enabledFlag);
        }
    }

    /**
     * @return the initial configuration file for logging, if configured or {@code null}
     */
    @VisibleForTesting
    static File getInitialConfigFile() {
        String configFileValue = null != System.getProperty(INSPECTIT_LOGGING_CONFIG_FILE_SYSTEM) ?
                System.getProperty(INSPECTIT_LOGGING_CONFIG_FILE_SYSTEM) : getEnvironment.apply(INSPECTIT_LOGGING_CONFIG_FILE);

        if (configFileValue != null) return new File(configFileValue);
        return null;
    }

    /**
     * Get the input stream of the used logging configuration file. <br>
     * First, we check the file, specified by the current {@link InspectitConfig}.<br>
     * Then, we try to read the initially set file. <br>
     * At the end, we fall back to inspectit default logging config.
     *
     * @param config the current inspectIT config
     *
     * @return the input stream of the used logging configuration file
     */
    private static InputStream getConfigFileInputStream(InspectitConfig config) {
        Optional<InputStream> inputStream = getCurrentConfigFileInputStream(config);
        if (inputStream.isPresent()) return inputStream.get();

        return getInitialConfigFileInputStream()
                .orElse(LogbackInitializer.class.getResourceAsStream("/inspectit-logback.xml"));
    }

    /**
     * @param config the current inspectIT configuration
     *
     * @return the input stream of the currently referenced logging config file
     */
    private static Optional<InputStream> getCurrentConfigFileInputStream(InspectitConfig config) {
        return Optional.ofNullable(config)
                .map(InspectitConfig::getLogging)
                .map(LoggingSettings::getConfigFile)
                .filter(Resource::exists)
                .filter(Resource::isFile)
                .filter(Resource::isReadable)
                .flatMap(r -> {
                    try {
                        return Optional.of(r.getInputStream());
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }

    /**
     * @return the input stream of the initially configured logging config file
     */
    private static Optional<InputStream> getInitialConfigFileInputStream() {
        File initialConfigFile = getInitialConfigFile();
        if (initialConfigFile == null) return Optional.empty();

        if(!initialConfigFile.exists() || !initialConfigFile.canRead()) {
            System.err.println("Could not read initial logging configuration file: " + initialConfigFile);
            return Optional.empty();
        }

        try {
            return Optional.of(Files.newInputStream(initialConfigFile.toPath()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Overwrites the properties with the values from {@link InspectitConfig}.
     *
     * @param config the current inspectit config
     */
    private static void setPropertiesFromConfig(InspectitConfig config) {
        consoleEnabled = config.getLogging().getConsole().isEnabled();
        fileEnabled = config.getLogging().getFile().isEnabled();

        LoggingSettings loggingSettings = config.getLogging();
        // path
        if (null != loggingSettings.getFile().getPath()) {
            setSystemProperty(INSPECTIT_LOG_PATH, loggingSettings.getFile().getPath().toAbsolutePath().toString());
        } else {
            clearSystemProperty(INSPECTIT_LOG_PATH);
        }

        // include service name
        if (loggingSettings.getFile().isIncludeServiceName()) {
            setSystemProperty(INSPECTIT_LOG_SERVICE_NAME, String.format("[%s]", config.getServiceName()));
        } else {
            setSystemProperty(INSPECTIT_LOG_SERVICE_NAME, "");
        }

        // level
        if (loggingSettings.isTrace()) {
            setSystemProperty(INSPECTIT_LOG_LEVEL, "TRACE");
        } else if (loggingSettings.isDebug()) {
            setSystemProperty(INSPECTIT_LOG_LEVEL, "DEBUG");
        } else {
            clearSystemProperty(INSPECTIT_LOG_LEVEL);
        }

        // console pattern
        if (StringUtils.isNotBlank(loggingSettings.getConsole().getPattern())) {
            setSystemProperty(INSPECTIT_LOG_CONSOLE_PATTERN, loggingSettings.getConsole().getPattern());
        } else {
            clearSystemProperty(INSPECTIT_LOG_CONSOLE_PATTERN);
        }

        // file pattern
        if (StringUtils.isNotBlank(loggingSettings.getFile().getPattern())) {
            setSystemProperty(INSPECTIT_LOG_FILE_PATTERN, loggingSettings.getFile().getPattern());
        } else {
            clearSystemProperty(INSPECTIT_LOG_FILE_PATTERN);
        }
    }

    /**
     * Sets system property if value is not null and report if the set was done.
     *
     * @param name  property name
     * @param value property value
     *
     * @return if set was done
     */
    private static boolean setSystemProperty(String name, String value) {
        if (value != null) {
            System.setProperty(name, value);
            return true;
        }
        return false;
    }

    /**
     * Clears system property and report if the clear was done.
     *
     * @param name property name
     *
     * @return true if property was set and then cleared
     */
    private static boolean clearSystemProperty(String name) {
        if (System.getProperty(name) != null) {
            System.clearProperty(name);
            return true;
        }
        return false;
    }

    /**
     * Custom Filter used in {@code inspectit-logback.xml}
     */
    public static class ConsoleFilter extends Filter {

        @Override
        public FilterReply decide(Object event) {
            if (consoleEnabled) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }
    }

    /**
     * Custom Filter used in {@code inspectit-logback.xml}
     */
    public static class FileFilter extends Filter {

        @Override
        public FilterReply decide(Object event) {
            if (fileEnabled) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }
    }
}
