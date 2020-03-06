package rocks.inspectit.ocelot.config.conversion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.MetricRecordingSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class InspectitConfigConversionServiceTest {

    InspectitConfigConversionService converter = InspectitConfigConversionService.getInstance();

    @Nested
    class MetricsRecordingSettings {

        private final TypeDescriptor TARGET = TypeDescriptor.valueOf(MetricRecordingSettings.class);

        @Test
        void fromDouble() {
            MetricRecordingSettings result = (MetricRecordingSettings) converter.convert(42.42, TARGET);

            assertThat(result.getValue()).isEqualTo("42.42");
        }

        @Test
        void fromInteger() {
            MetricRecordingSettings result = (MetricRecordingSettings) converter.convert(42, TARGET);

            assertThat(result.getValue()).isEqualTo("42");
        }

        @Test
        void fromString() {
            MetricRecordingSettings result = (MetricRecordingSettings) converter.convert("data_key", TARGET);

            assertThat(result.getValue()).isEqualTo("data_key");
        }

        @Test
        void fromEmptyString() {
            MetricRecordingSettings result = (MetricRecordingSettings) converter.convert("", TARGET);

            assertThat(result.getValue()).isEqualTo("");
        }
    }
}
