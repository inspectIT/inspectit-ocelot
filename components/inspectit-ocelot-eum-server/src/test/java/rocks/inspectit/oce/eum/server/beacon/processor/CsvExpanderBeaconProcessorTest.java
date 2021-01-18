package rocks.inspectit.oce.eum.server.beacon.processor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@ExtendWith(MockitoExtension.class)
class CsvExpanderBeaconProcessorTest {

    @InjectMocks
    private CsvExpanderBeaconProcessor processor;

    @Nested
    public class Process {

        @Test
        public void noRtBmrAttribute() {
            Beacon beacon = Beacon.of(Collections.singletonMap("key", ""));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("key", ""));
        }

        @Test
        public void rtBmrWithoutPatternAttribute() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", ""));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("rt.bmr", ""));
        }

        @Test
        public void rtBmrWithPatternAttribute() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("rt.bmr", "123"), entry("rt.bmr.0", "123"), entry("rt.bmr.sum", "123"));
        }

        @Test
        public void tOtherWithMultiplePatternAttributes() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,321"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("rt.bmr", "123,321"), entry("rt.bmr.0", "123"), entry("rt.bmr.1", "321"), entry("rt.bmr.sum", "444"));
        }
    }
}