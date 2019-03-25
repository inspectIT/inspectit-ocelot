package rocks.inspectit.ocelot.core.logging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigurationChangeListenerIntTest {

    @Nested
    @DirtiesContext
    class Update extends SpringTestBase {

        @Test
        public void loggingProperty() {
            updateProperties(properties -> properties.withProperty("inspectit.logging.trace", Boolean.TRUE));

            // make sure config was updated
            assertThat(System.getProperty("INSPECTIT_LOG_LEVEL")).isEqualTo("TRACE");
        }

        @Test
        public void serviceName() {
            String value = "my-service-name";
            updateProperties(properties -> {
                properties.withProperty("inspectit.service-name", value);
            });

            // make sure config was updated
            assertThat(System.getProperty("INSPECTIT_LOG_SERVICE_NAME")).contains(value);
        }

        @Test
        public void notRelated() {
            String invalid = "something_not_valid";
            System.setProperty("INSPECTIT_LOG_SERVICE_NAME", invalid);

            // make sure that we are not initializing the logging thus invalid value is still in System props
            updateProperties(properties -> properties.withProperty("inspectit.thread-pool-size", 3));

            assertThat(System.clearProperty("INSPECTIT_LOG_SERVICE_NAME")).isEqualTo(invalid);
        }

    }

}