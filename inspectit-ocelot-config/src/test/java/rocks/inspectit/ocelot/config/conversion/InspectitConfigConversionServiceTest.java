package rocks.inspectit.ocelot.config.conversion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import rocks.inspectit.ocelot.config.model.exporters.TransportProtocol;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class InspectitConfigConversionServiceTest {

    private final InspectitConfigConversionService converter = InspectitConfigConversionService.getInstance();

    @Nested
    class TransportProtocolConverter {

        private final TypeDescriptor TARGET = TypeDescriptor.valueOf(TransportProtocol.class);

        @Test
        void fromString() {
            TransportProtocol result = (TransportProtocol) converter.convert("grpc", TARGET);

            assertThat(result).isEqualTo(TransportProtocol.GRPC);
        }

        @Test
        void fromEmptyString() {
            TransportProtocol result = (TransportProtocol) converter.convert("", TARGET);

            assertThat(result).isEqualTo(null);
        }
    }

    @Nested
    class DurationConverter {

        private final InspectitConfigConversionService converter = InspectitConfigConversionService.getParserInstance();

        private final TypeDescriptor TARGET = TypeDescriptor.valueOf(Duration.class);

        private final Duration DUMMY_DURATION = Duration.ofHours(1);

        @Test
        void fromPlaceholder() {
            Duration result = (Duration) converter.convert("${placeholder}", TARGET);

            assertThat(result).isEqualTo(DUMMY_DURATION);
        }

        @Test
        void fromString() {
            Duration result = (Duration) converter.convert("60s", TARGET);

            assertThat(result).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        void fromEmptyString() {
            Duration result = (Duration) converter.convert("", TARGET);

            assertThat(result).isEqualTo(DUMMY_DURATION);
        }
    }
}
