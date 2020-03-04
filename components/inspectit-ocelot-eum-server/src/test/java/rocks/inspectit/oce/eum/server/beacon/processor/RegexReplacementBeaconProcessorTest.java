package rocks.inspectit.oce.eum.server.beacon.processor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.eum.server.beacon.Beacon;
import rocks.inspectit.oce.eum.server.configuration.model.EumServerConfiguration;
import rocks.inspectit.oce.eum.server.configuration.model.EumTagsSettings;
import rocks.inspectit.oce.eum.server.configuration.model.RegexTagSettings;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RegexReplacementBeaconProcessorTest {

    @Nested
    class Process {

        @Test
        void errorOnCyclicDependency() {
            RegexTagSettings first = RegexTagSettings.builder().input("second").regex(".*").replacement("").build();
            RegexTagSettings second = RegexTagSettings.builder().input("third").regex(".*").replacement("").build();
            RegexTagSettings third = RegexTagSettings.builder().input("first").regex(".*").replacement("").build();

            Map<String, RegexTagSettings> regexSettings = new LinkedHashMap<>();
            regexSettings.put("first", first);
            regexSettings.put("second", second);
            regexSettings.put("third", third);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setRegex(regexSettings);

            assertThatThrownBy(() -> new RegexReplacementBeaconProcessor(conf)).hasMessageStartingWith("Cyclic");
        }


        @Test
        void dependenciesRespected() {
            RegexTagSettings first = RegexTagSettings.builder().input("in").regex("Hello World").replacement("Bye World").build();
            RegexTagSettings second = RegexTagSettings.builder().input("first").regex("Bye World").replacement("Bye Earth").build();

            Map<String, RegexTagSettings> invalidOrder = new LinkedHashMap<>();
            invalidOrder.put("second", second);
            invalidOrder.put("first", first);

            EumServerConfiguration conf = new EumServerConfiguration();
            conf.setTags(new EumTagsSettings());
            conf.getTags().setRegex(invalidOrder);

            RegexReplacementBeaconProcessor proc = new RegexReplacementBeaconProcessor(conf);

            Beacon result = proc.process(Beacon.of(ImmutableMap.of("in", "Hello World")));

            assertThat(result.get("in")).isEqualTo("Hello World");
            assertThat(result.get("first")).isEqualTo("Bye World");
            assertThat(result.get("second")).isEqualTo("Bye Earth");
        }
    }
}
