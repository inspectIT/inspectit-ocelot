package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.BeaconTagSettings;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.configuration.model.EumTagsSettings;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegexReplacementBeaconProcessorTest {

    @Nested
    class Process {

        @Test
        void errorOnCyclicDependency() {
            BeaconTagSettings first = BeaconTagSettings.builder().input("second").regex(".*").replacement("").build();
            BeaconTagSettings second = BeaconTagSettings.builder().input("third").regex(".*").replacement("").build();
            BeaconTagSettings third = BeaconTagSettings.builder().input("first").regex(".*").replacement("").build();

            Map<String, BeaconTagSettings> regexSettings = new LinkedHashMap<>();
            regexSettings.put("first", first);
            regexSettings.put("second", second);
            regexSettings.put("third", third);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setBeacon(regexSettings);

            assertThatThrownBy(() -> new RegexReplacementBeaconProcessor(conf)).hasMessageStartingWith("Cyclic");
        }


        @Test
        void dependenciesRespected() {
            BeaconTagSettings first = BeaconTagSettings.builder().input("in").regex("Hello World").replacement("Bye World").build();
            BeaconTagSettings second = BeaconTagSettings.builder().input("first").regex("Bye World").replacement("Bye Earth").build();

            Map<String, BeaconTagSettings> invalidOrder = new LinkedHashMap<>();
            invalidOrder.put("second", second);
            invalidOrder.put("first", first);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setBeacon(invalidOrder);

            RegexReplacementBeaconProcessor proc = new RegexReplacementBeaconProcessor(conf);

            Beacon result = proc.process(Beacon.of(ImmutableMap.of("in", "Hello World")));

            assertThat(result.get("in")).isEqualTo("Hello World");
            assertThat(result.get("first")).isEqualTo("Bye World");
            assertThat(result.get("second")).isEqualTo("Bye Earth");
        }


        @Test
        void copyWithoutRegexSupported() {
            BeaconTagSettings first = BeaconTagSettings.builder().input("in").build();

            Map<String, BeaconTagSettings> beaconTags = new LinkedHashMap<>();
            beaconTags.put("first", first);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setBeacon(beaconTags);

            RegexReplacementBeaconProcessor proc = new RegexReplacementBeaconProcessor(conf);

            Beacon result = proc.process(Beacon.of(ImmutableMap.of("in", "Hello World")));

            assertThat(result.get("first")).isEqualTo("Hello World");
        }


        @Test
        void inplaceChangeSupported() {
            BeaconTagSettings first = BeaconTagSettings.builder().input("value").regex("Hello World").replacement("Goodbye").build();

            Map<String, BeaconTagSettings> beaconTags = new LinkedHashMap<>();
            beaconTags.put("value", first);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setBeacon(beaconTags);

            RegexReplacementBeaconProcessor proc = new RegexReplacementBeaconProcessor(conf);

            Beacon result = proc.process(Beacon.of(ImmutableMap.of("value", "Hello World")));

            assertThat(result.get("value")).isEqualTo("Goodbye");
        }
    }
}
