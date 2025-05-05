package rocks.inspectit.ocelot.core.logging.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static rocks.inspectit.ocelot.core.logging.logback.LoggingProperties.*;

@ExtendWith(MockitoExtension.class)
public class LogbackInitializerTest {

    @Nested
    class IsConsoleInitiallyEnabled {

        @AfterEach
        public void afterEach() {
            System.clearProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM);
            LogbackInitializer.getEnvironment = System::getenv;
        }

        @Test
        public void withoutSystemProperty() {
            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsTrue() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM, "true");

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsFalse() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM, "false");

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void systemPropertyIsInvalid() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM, "no-boolean");

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void environmentIsTrue() {
            LogbackInitializer.getEnvironment = envKey -> "true";

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void environmentIsFalse() {
            LogbackInitializer.getEnvironment = envKey -> "false";

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void environmentIsInvalid() {
            LogbackInitializer.getEnvironment = envKey -> "no-boolean";

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void testPropertyEnvPriority() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED_SYSTEM, "false");
            LogbackInitializer.getEnvironment = envKey -> "true";

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }
    }

    @Nested
    class IsFileInitiallyEnabled {

        @AfterEach
        public void afterEach() {
            System.clearProperty(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM);
            LogbackInitializer.getEnvironment = System::getenv;
        }

        @Test
        public void withoutSystemProperty() {
            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsTrue() {
            System.setProperty(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM, "true");

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsFalse() {
            System.setProperty(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM, "false");

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void systemPropertyIsInvalid() {
            System.setProperty(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM, "no-boolean");

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void environmentIsTrue() {
            LogbackInitializer.getEnvironment = envKey -> "true";

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void environmentIsFalse() {
            LogbackInitializer.getEnvironment = envKey -> "false";

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void environmentIsInvalid() {
            LogbackInitializer.getEnvironment = envKey -> "no-boolean";

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void testPropertyEnvPriority() {
            System.setProperty(INSPECTIT_LOGGING_FILE_ENABLED_SYSTEM, "false");
            LogbackInitializer.getEnvironment = envKey -> "true";

            boolean result = LogbackInitializer.isFileInitiallyEnabled();

            assertThat(result).isFalse();
        }
    }
}