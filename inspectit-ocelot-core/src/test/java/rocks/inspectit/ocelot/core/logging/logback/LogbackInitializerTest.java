package rocks.inspectit.ocelot.core.logging.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static rocks.inspectit.ocelot.core.logging.logback.LogbackInitializer.INSPECTIT_LOGGING_CONSOLE_ENABLED;

@ExtendWith(MockitoExtension.class)
public class LogbackInitializerTest {

    @Nested
    class IsConsoleInitiallyEnabled {

        @AfterEach
        public void afterEach() {
            System.clearProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED);
            LogbackInitializer.getEnvironment = System::getenv;
        }

        @Test
        public void withoutSystemProperty() {
            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsTrue() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED, "true");

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isTrue();
        }

        @Test
        public void systemPropertyIsFalse() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED, "false");

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }

        @Test
        public void systemPropertyIsInvalid() {
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED, "no-boolean");

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
            System.setProperty(INSPECTIT_LOGGING_CONSOLE_ENABLED, "false");
            LogbackInitializer.getEnvironment = envKey -> "true";

            boolean result = LogbackInitializer.isConsoleInitiallyEnabled();

            assertThat(result).isFalse();
        }
    }

}