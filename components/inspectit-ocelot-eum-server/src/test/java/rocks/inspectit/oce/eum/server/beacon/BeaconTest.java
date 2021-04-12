package rocks.inspectit.oce.eum.server.beacon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class BeaconTest {

    @Nested
    public class Contains {

        private Beacon beacon;

        private Beacon beaconWithHeader;

        private String headerPrefix = "client.header";

        @BeforeEach
        private void before() {
            HashMap<String, String> beaconMap = new HashMap<>();
            beaconMap.put("first", "1");
            beaconMap.put("second", "2");
            beacon = Beacon.of(beaconMap);

            HashMap<String, String> headerMap = new HashMap<>();
            headerMap.put("firstHeader", "1");
            headerMap.put("secondHeader", "2");
            beaconWithHeader = Beacon.of(beaconMap, headerMap);
        }

        @Test
        public void beaconContainsField() {
            boolean result = beacon.contains("first");

            assertThat(result).isTrue();
        }

        @Test
        public void beaconWithHeaderContainsField() {
            boolean result = beaconWithHeader.contains(headerPrefix + ".firstHeader");

            assertThat(result).isTrue();
        }

        @Test
        public void merge() {
            HashMap<String, String> map = new HashMap<>();
            map.put("first", "3");
            map.put("third", "4");
            Beacon beacon1 = Beacon.of(map);
            beacon = Beacon.merge(beacon, beacon1);
            assertThat(beacon.contains(Arrays.asList("first", "second", "third"))).isTrue();
            assertThat(beacon.get("first")).isEqualTo("3");
        }

        @Test
        public void mergeWithHeader() {
            HashMap<String, String> map = new HashMap<>();
            map.put("first", "3");
            map.put("third", "4");
            Beacon beacon1 = Beacon.of(map);
            beaconWithHeader = Beacon.merge(beaconWithHeader, beacon1);
            assertThat(beaconWithHeader.contains(Arrays.asList("first", "second", "third", headerPrefix + ".firstHeader", headerPrefix + ".secondHeader")))
                    .isTrue();
            assertThat(beaconWithHeader.get("first")).isEqualTo("3");
        }

        @Test
        public void beaconContainsFields() {
            boolean result = beacon.contains("first", "second");

            assertThat(result).isTrue();
        }

        @Test
        public void beaconWithHeaderContainsFields() {
            boolean result = beaconWithHeader.contains("first", "second", headerPrefix + ".firstHeader", headerPrefix + ".secondHeader");

            assertThat(result).isTrue();
        }

        @Test
        public void beaconDoesNotContainField() {
            boolean result = beacon.contains("third");

            assertThat(result).isFalse();
        }

        @Test
        public void beaconWithHeaderDoesNotContainField() {
            boolean result = beaconWithHeader.contains(headerPrefix + ".thirdHeader");

            assertThat(result).isFalse();
        }

        @Test
        public void beaconDoesNotContainFields() {
            boolean result = beacon.contains("first", "third");

            assertThat(result).isFalse();
        }

        @Test
        public void beaconWithHeaderDoesNotContainFields() {
            boolean result = beaconWithHeader.contains(headerPrefix + ".firstHeader", headerPrefix + ".thirdHeader");

            assertThat(result).isFalse();
        }

        @Test
        public void beaconContainsFieldsAsList() {
            boolean result = beacon.contains(Arrays.asList("first", "second"));

            assertThat(result).isTrue();
        }

        @Test
        public void beaconWithHeaderContainsFieldsAsList() {
            boolean result = beaconWithHeader.contains(Arrays.asList("first", "second", headerPrefix + ".firstHeader", headerPrefix + ".secondHeader"));

            assertThat(result).isTrue();
        }

        @Test
        public void beaconDoesNotContainFieldsAsList() {
            boolean result = beacon.contains(Arrays.asList("first", "third"));

            assertThat(result).isFalse();
        }

        @Test
        public void beaconWithHeaderDoesNotContainFieldsAsList() {
            boolean result = beaconWithHeader.contains(Arrays.asList(headerPrefix + ".firstHeader", headerPrefix + ".thirdHeader"));

            assertThat(result).isFalse();
        }
    }
}