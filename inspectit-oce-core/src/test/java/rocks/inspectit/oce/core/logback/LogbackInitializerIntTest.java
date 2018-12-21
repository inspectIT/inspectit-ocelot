package rocks.inspectit.oce.core.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import rocks.inspectit.oce.core.SpringTestBase;
import rocks.inspectit.oce.core.config.InspectitEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackInitializerIntTest {

    private static final String EXCEPTIONS_LOG_FILE = "exceptions.log";
    private static final String AGENT_LOG_FILE = "agent.log";

    @Nested
    @DirtiesContext
    class Defaults extends SpringTestBase {

        private final String TMP_DIR = "tmp";

        @Autowired
        InspectitEnvironment environment;

        @BeforeEach
        void initTemp() {
            System.setProperty("java.io.tmpdir", TMP_DIR);
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

            String testMessage = "trace message";
            Logger logger = LoggerFactory.getLogger(OverwrittenDefaults.class);
            logger.info(testMessage);

            Path output = Paths.get(TMP_DIR, "inspectit-oce");
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

            Path output = Paths.get(TMP_DIR, "inspectit-oce");
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


        @AfterEach
        void clean() throws Exception {
            System.clearProperty("java.io.tmpdir");

            Path output = Paths.get(TMP_DIR);
            Files.walk(output)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.deleteIfExists(output);
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
            "inspectit.logging.file.path=" + OverwrittenDefaults.MY_CUSTOM_PATH,
            "inspectit.logging.file.include-service-name=false",
    })
    @DirtiesContext
    class OverwrittenDefaults extends SpringTestBase {

        static final String MY_CUSTOM_PATH = "my/custom/path";

        @Autowired
        InspectitEnvironment environment;

        @Test
        void propertiesSet() {
            LogbackInitializer.initLogging(environment.getCurrentConfig());

            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_LEVEL)).isEqualTo("TRACE");
            assertThat(System.getProperty(LogbackInitializer.INSPECTIT_LOG_PATH)).isEqualTo(Paths.get(MY_CUSTOM_PATH).toAbsolutePath().toString());
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

            Path output = Paths.get(MY_CUSTOM_PATH);
            Optional<Path> agentLog = Files.walk(output)
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
            Path output = Paths.get("my");
            Files.walk(output)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            Files.deleteIfExists(output);
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
            Files.deleteIfExists(OUTPUT_FILE);
        }

    }

}