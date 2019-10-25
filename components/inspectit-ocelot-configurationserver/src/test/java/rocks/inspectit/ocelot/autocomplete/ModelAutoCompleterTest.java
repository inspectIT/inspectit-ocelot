package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelAutoCompleterTest {

    ModelAutoCompleter completer;

    @BeforeEach
    public void setCompleter() {
        completer = new ModelAutoCompleter();
    }


    @Nested
    public class CheckPropertyExists {
        @Test
        void checkFirstLevel() {
            List<String> input = Arrays.asList("inspectit", "instrumentation");
            List<String> expected = Arrays.asList("actions",
                    "data",
                    "exclude-lambdas",
                    "ignored-bootstrap-packages",
                    "ignored-packages",
                    "internal",
                    "rules",
                    "scopes",
                    "special");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void pastMap() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "scopes", "my-key");
            List<String> expected = Arrays.asList("advanced",
                    "interfaces",
                    "methods",
                    "superclass",
                    "type");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void pastList() {
            List<String> input = Arrays.asList("inspectit", "");
            ArrayList<String> expected = new ArrayList<>(Arrays.asList("config",
                    "exporters",
                    "instrumentation",
                    "logging",
                    "metrics",
                    "publish-open-census-to-bootstrap",
                    "self-monitoring",
                    "service-name",
                    "tags",
                    "thread-pool-size",
                    "tracing"));

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void endsInWildcard() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex");
            ArrayList<String> expected = new ArrayList<>();

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "data", "method_duration", "is-tag");
            ArrayList<String> expected = new ArrayList<>();

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void classPropertyNotAdded() {
            List<String> input = Arrays.asList("inspectit.");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).doesNotContainSequence("class");
        }

        @Test
        void emptyString() {
            List<String> input = Arrays.asList("");
            List<String> expected = Arrays.asList("inspectit");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void startsLikeInspectit() {
            List<String> input = Arrays.asList("inspe");
            List<String> expected = Arrays.asList("inspectit");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void startsNotLikeInspectit() {
            List<String> input = Arrays.asList("test");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(Collections.emptyList());
        }

        @Test
        void startsNotWithInspectit() {
            List<String> input = Arrays.asList("test", "antotherTest");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEqualTo(Collections.emptyList());
        }
    }

    @Nested
    public class GetProperties {
        @Test
        void getPropertiesInspectit() {
            List<String> expected = Arrays.asList("config",
                    "exporters",
                    "instrumentation",
                    "logging",
                    "metrics",
                    "publish-open-census-to-bootstrap",
                    "self-monitoring",
                    "service-name",
                    "tags",
                    "thread-pool-size",

                    "tracing");

            List<String> result = completer.getProperties(InspectitConfig.class);

            assertThat(result).isEqualTo(expected);
        }
    }
}
