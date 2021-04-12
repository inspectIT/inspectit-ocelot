package rocks.inspectit.oce.eum.server.beacon.processor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.oce.eum.server.beacon.Beacon;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BmrBeaconProcessorTest {

    @InjectMocks
    private BmrBeaconProcessor processor;

    @Nested
    public class Process {

        private final String[] keyNames = Arrays.stream(BmrBeaconProcessor.VALUE_NAMES)
                .map(valueName -> BmrBeaconProcessor.ATTRIBUTE_KEY + "." + valueName)
                .toArray(String[]::new);

        private final int maxExpectedSize = keyNames.length + 1;

        @Test
        public void noRtBmrAttribute() {
            Beacon beacon = Beacon.of(Collections.singletonMap("key", ""));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("key", ""));
        }

        @Test
        public void rtBmrWithoutValues() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", ""));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsOnly(entry("rt.bmr", ""));
        }

        @Test
        public void rtBmrWithOneValue() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .containsOnly(entry("rt.bmr", "123"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseEnd", "0"), entry("rt.bmr.responseStart", "0"), entry("rt.bmr.requestStart", "0"), entry("rt.bmr.connectEnd", "0"), entry("rt.bmr.secureConnectionStart", "0"), entry("rt.bmr.connectStart", "0"), entry("rt.bmr.domainLookupEnd", "0"), entry("rt.bmr.domainLookupStart", "0"), entry("rt.bmr.redirectEnd", "0"), entry("rt.bmr.redirectStart", "0"));
        }

        @Test
        public void rtBmrWithInvalidValue() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "foo"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).doesNotContainKey("rt.bmr.startTime")
                    .hasSize(maxExpectedSize - 1)
                    .contains(entry("rt.bmr", "foo"));
        }

        @Test
        public void rtBmrWithZeroValue() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "0"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .contains(entry("rt.bmr", "0"), entry("rt.bmr.startTime", "0"));
        }

        @Test
        public void rtBmrWithZeroSum() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "0,0"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .contains(entry("rt.bmr", "0,0"), entry("rt.bmr.startTime", "0"), entry("rt.bmr.responseEnd", "0"));
        }

        @Test
        public void rtBmrWithMultipleValues() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,321"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .contains(entry("rt.bmr", "123,321"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseEnd", "321"));
        }

        @Test
        public void rtBmrMultipleValuesWithOneEmptyValue() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,,321"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .contains(entry("rt.bmr", "123,,321"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseEnd", "0"), entry("rt.bmr.responseStart", "321"));
        }

        @Test
        public void rtBmrMultipleValuesWithOneInvalidValue() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,bar,321"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).doesNotContainKey("rt.bmr.responseEnd")
                    .hasSize(maxExpectedSize - 1)
                    .contains(entry("rt.bmr", "123,bar,321"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseStart", "321"));
        }

        @Test
        public void rtBmrMultipleValuesWithTwoInvalidValues() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,bar,321,foo"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).doesNotContainKeys("rt.bmr.responseEnd", "rt.bmr.requestStart")
                    .hasSize(maxExpectedSize - 2)
                    .contains(entry("rt.bmr", "123,bar,321,foo"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseStart", "321"));
        }

        @Test
        public void rtBmrAllValues() {
            Beacon beacon = Beacon.of(Collections.singletonMap("rt.bmr", "123,120,,,,1,,,7,,,"));

            Beacon result = processor.process(beacon);

            assertThat(result.toMap()).containsKeys(keyNames)
                    .hasSize(maxExpectedSize)
                    .contains(entry("rt.bmr", "123,120,,,,1,,,7,,,"), entry("rt.bmr.startTime", "123"), entry("rt.bmr.responseEnd", "120"), entry("rt.bmr.secureConnectionStart", "1"), entry("rt.bmr.domainLookupStart", "7"));
        }
    }
}