package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.logging.LoggingSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Main logback initializer. Method {@link #initLogging(InspectitConfig)} can be called whenever inspectIT configuration is changed in order to update the logging settings.
 *
 * @author Ivan Senic
 */
public class LogbackInitializer {

    /**
     * The name of the System property that contains the log path.
     */
    static final String INSPECTIT_LOG_LEVEL = "INSPECTIT_LOG_LEVEL";

    /**
     * The name of the System property that contains the log path.
     */
    static final String INSPECTIT_LOG_PATH = "INSPECTIT_LOG_PATH";

    /**
     * The name of the System property that contains this service name.
     */
    static final String INSPECTIT_LOG_SERVICE_NAME = "INSPECTIT_LOG_SERVICE_NAME";

    /**
     * The name of the System property that contains this console pattern.
     */
    static final String INSPECTIT_LOG_CONSOLE_PATTERN = "INSPECTIT_LOG_CONSOLE_PATTERN";

    /**
     * The name of the System property that contains this console pattern.
     */
    static final String INSPECTIT_LOG_FILE_PATTERN = "INSPECTIT_LOG_FILE_PATTERN";

    // flags for the filters
    static boolean consoleEnabled = true;
    static boolean fileEnabled = true;
    static boolean selfMonitoringEnabled = true;


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
            recorder.recordEvents(getConfigFileInputStream(config));
            configurator.doConfigure(recorder.getSaxEventList());
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private static InputStream getConfigFileInputStream(InspectitConfig config) {
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
                })
                .orElse(LogbackInitializer.class.getResourceAsStream("/inspectit-logback.xml"));
    }

    private static void setPropertiesFromConfig(InspectitConfig config) {
        consoleEnabled = config.getLogging().getConsole().isEnabled();
        fileEnabled = config.getLogging().getFile().isEnabled();
        selfMonitoringEnabled = config.getSelfMonitoring().isEnabled();

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
     * @return true if property was set and then cleared
     */
    private static boolean clearSystemProperty(String name) {
        if (System.getProperty(name) != null) {
            System.clearProperty(name);
            return true;
        }
        return false;
    }

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

    public static class SelfMonitoringFilter extends Filter {

        @Override
        public FilterReply decide(Object event) {
            if (selfMonitoringEnabled) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }
    }

}
