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

        @BeforeEach
        private void before() {
            HashMap<String, String> map = new HashMap<>();
            map.put("first", "1");
            map.put("second", "2");
            beacon = Beacon.of(map);
        }

        @Test
        public void containsField() {
            boolean result = beacon.contains("first");

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
        public void containsFields() {
            boolean result = beacon.contains("first", "second");

            assertThat(result).isTrue();
        }

        @Test
        public void doesNotContainField() {
            boolean result = beacon.contains("third");

            assertThat(result).isFalse();
        }

        @Test
        public void doesNotContainFields() {
            boolean result = beacon.contains("first", "third");

            assertThat(result).isFalse();
        }

        @Test
        public void containsFieldsAsList() {
            boolean result = beacon.contains(Arrays.asList("first", "second"));

            assertThat(result).isTrue();
        }

        @Test
        public void doesNotContainFieldsAsList() {
            boolean result = beacon.contains(Arrays.asList("first", "third"));

            assertThat(result).isFalse();
        }
    }
}