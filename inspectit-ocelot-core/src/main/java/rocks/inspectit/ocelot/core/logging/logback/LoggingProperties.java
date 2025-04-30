package rocks.inspectit.ocelot.core.logging.logback;

import rocks.inspectit.ocelot.config.model.logging.LoggingSettings;

/**
 * Stores all properties, which are referenced inside our default logging configuration {@code inspectit-logback.xml}.
 * The properties can be overwritten by the {@link LoggingSettings} at runtime.
 * Even though we (mostly) use the ENV variable naming convention, the variable are used as system properties
 * (see {@link LogbackInitializer}).
 */
public class LoggingProperties {

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

    /**
     * Allow disabling the console log before the log system is initialized.
     */
    static final String INSPECTIT_LOGGING_CONSOLE_ENABLED = "INSPECTIT_LOGGING_CONSOLE_ENABLED";
    static final String INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM = "inspectit.logging.console.enabled";

    /**
     * Allow disabling the file log before the log system is initialized.
     */
    static final String INSPECTIT_LOGGING_FILE_ENABLED = "INSPECTIT_LOGGING_FILE_ENABLED";
    static final String INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM = "inspectit.logging.file.enabled";

    /**
     * Allow using custom logging config before the log system is initialized.
     * This property is NOT used in our default logging config.
     */
    static final String INSPECTIT_LOGGING_CONFIG_FILE = "INSPECTIT_LOGGING_CONFIG_FILE";
    static final String INSPECTIT_LOGGING_CONFIG_FILE_SYSTEM = "inspectit.logging.config-file";
}
