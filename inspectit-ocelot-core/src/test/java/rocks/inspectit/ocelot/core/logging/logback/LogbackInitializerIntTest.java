package rocks.inspectit.ocelot.core.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackInitializerIntTest {

    private static final String EXCEPTIONS_LOG_FILE = "exceptions.log";
    private static final String AGENT_LOG_FILE = "agent.log";

    @Nested
    @DirtiesContext
    class Defaults extends SpringTestBase {

        private Path tempDirectory;

        @Autowired
        InspectitEnvironment environment;

        @BeforeEach
        void initTemp() throws IOException {
            tempDirectory = Files.createTempDirectory("ocelot-logback");
            System.setProperty("java.io.tmpdir", tempDirectory.toString());
        }

        @Test
        void propertiesSet() {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_LEVEL)).isNull();
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_PATH)).isNull();
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_SERVICE_NAME)).isEqualTo("[" + environment.getCurrentConfig().getServiceName() + "]");
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_CONSOLE_PATTERN)).isNull();
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_FILE_PATTERN)).isNull();

            assertThat(LogbackInitializer.consoleEnabled).isTrue();
            assertThat(LogbackInitializer.fileEnabled).isTrue();
        }

        @Test
        void logMessage() throws Exception {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            String testMessage = "info message";
            Logger logger = LoggerFactory.getLogger(OverwrittenDefaults.class);
            logger.info(testMessage);

            Path output = Paths.get(tempDirectory.toString(), "inspectit-ocelot");
            Optional<Path> agentLog = Files.walk(output)
                    .filter(p -> p.endsWith(AGENT_LOG_FILE))
                    .findFirst();
            assertThat(agentLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            assertThat(Files.lines(p).anyMatch(l -> l.contains(testMessage))).isTrue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // nothing to error log due to the level
            Optional<Path> errorLog = Files.walk(output)
                    .filter(p -> p.endsWith(EXCEPTIONS_LOG_FILE))
                    .findFirst();
            assertThat(errorLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            assertThat(Files.readAllLines(p)).isEmpty();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        @Test
        void logExceptionMessage() throws Exception {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            String testMessage = "exception message";
            String exceptionMessage = "extra exception stuff";
            Exception exception = new Exception(exceptionMessage);
            Logger logger = LoggerFactory.getLogger(OverwrittenDefaults.class);
            logger.warn(testMessage, exception);

            Path output = Paths.get(tempDirectory.toString(), "inspectit-ocelot");
            Optional<Path> agentLog = Files.walk(output)
                    .filter(p -> p.endsWith(AGENT_LOG_FILE))
                    .findFirst();
            assertThat(agentLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p);
                            assertThat(lines.stream().anyMatch(l -> l.contains(testMessage))).isTrue();
                            assertThat(lines.stream().anyMatch(l -> l.contains(exceptionMessage))).isTrue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            Optional<Path> errorLog = Files.walk(output)
                    .filter(p -> p.endsWith(EXCEPTIONS_LOG_FILE))
                    .findFirst();
            assertThat(errorLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p);
                            assertThat(lines.stream().anyMatch(l -> l.contains(testMessage))).isTrue();
                            assertThat(lines.stream().anyMatch(l -> l.contains(exceptionMessage))).isTrue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        @Test
        void logTraceMessage() throws Exception {
            LogbackInitializer.initLogging(environment.getCurrentConfig());
            updateProperties(properties -> properties.withProperty("inspectit.logging.trace", Boolean.TRUE));

            String testMessage = "trace message";
            Logger logger = LoggerFactory.getLogger(OverwrittenDefaults.class);
            logger.trace(testMessage);

            Path output = Paths.get(tempDirectory.toString(), "inspectit-ocelot");
            Optional<Path> agentLog = Files.walk(output)
                    .filter(p -> p.endsWith(AGENT_LOG_FILE))
                    .findFirst();
            assertThat(agentLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            assertThat(Files.lines(p).anyMatch(l -> l.contains(testMessage))).isTrue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // nothing to error log due to the level
            Optional<Path> errorLog = Files.walk(output)
                    .filter(p -> p.endsWith(EXCEPTIONS_LOG_FILE))
                    .findFirst();
            assertThat(errorLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            assertThat(Files.readAllLines(p)).isEmpty();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        @AfterEach
        void clean() {
            System.clearProperty("java.io.tmpdir");

            try {
                ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

                // Check for logback implementation of slf4j
                if (loggerFactory instanceof LoggerContext) {
                    LoggerContext context = (LoggerContext) loggerFactory;
                    context.reset();
                    context.stop();
                }

                FileUtils.deleteDirectory(tempDirectory.toFile());
            } catch (Exception e) {
                // ignored - this may happen on Windows if the log files are still locked by the unit test
            }
        }

    }

    @Nested
    @TestPropertySource(properties = {
            "inspectit.logging.trace=true",
            "inspectit.logging.debug=true",
            "inspectit.logging.console.enabled=false",
            "inspectit.logging.console.pattern=my-console-pattern",
            "inspectit.logging.file.enabled=false",
            "inspectit.logging.file.pattern=my-file-pattern",
            "inspectit.logging.file.include-service-name=false",
    })
    @DirtiesContext
    class OverwrittenDefaults extends SpringTestBase {

        Path tempDirectory;

        @Autowired
        InspectitEnvironment environment;

        @BeforeEach
        void beforeTest() throws IOException {
            tempDirectory = Files.createTempDirectory("ocelot-logback");
            environment.getCurrentConfig().getLogging().getFile().setPath(tempDirectory);
        }

        @Test
        void propertiesSet() {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_LEVEL)).isEqualTo("TRACE");
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_PATH)).isEqualTo(tempDirectory.toAbsolutePath().toString());
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_SERVICE_NAME)).isEmpty();
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_CONSOLE_PATTERN)).isEqualTo("my-console-pattern");
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_FILE_PATTERN)).isEqualTo("my-file-pattern");

            assertThat(LogbackInitializer.consoleEnabled).isFalse();
            assertThat(LogbackInitializer.fileEnabled).isFalse();
        }

        @Test
        void logMessageFileDisabled() throws Exception {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            String testMessage = "info message";
            Logger logger = LoggerFactory.getLogger(OverwrittenDefaults.class);
            logger.info(testMessage);

            Optional<Path> agentLog = Files.walk(tempDirectory)
                    .filter(p -> p.endsWith(AGENT_LOG_FILE))
                    .findFirst();

            // nothing written
            assertThat(agentLog)
                    .isPresent()
                    .hasValueSatisfying(p -> {
                        try {
                            assertThat(Files.readAllLines(p)).isEmpty();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        @AfterEach
        void clean() throws Exception {
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

            // Check for logback implementation of slf4j
            if (loggerFactory instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) loggerFactory;
                context.reset();
                context.stop();
            }

            FileUtils.deleteDirectory(tempDirectory.toFile());
        }
    }

    @Nested
    @TestPropertySource(properties = {
            "inspectit.logging.config-file=src/test/resources/test-logback.xml"
    })
    @DirtiesContext
    class CustomLogFile extends SpringTestBase {

        private final Path OUTPUT_FILE = Paths.get("test-agent.log");

        @Autowired
        InspectitEnvironment environment;

        @Test
        void logMessage() throws Exception {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            String testMessage = "test message with custom config file";
            Logger logger = LoggerFactory.getLogger(CustomLogFile.class);
            logger.info(testMessage);

            assertThat(Files.lines(OUTPUT_FILE).anyMatch(l -> l.contains(testMessage))).isTrue();
        }

        @AfterEach
        void clean() throws Exception {
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

            // Check for logback implementation of slf4j
            if (loggerFactory instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) loggerFactory;
                context.reset();
                context.stop();
            }

            Files.deleteIfExists(OUTPUT_FILE);
        }

    }

}