package rocks.inspectit.ocelot.autocomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
            List<String> output = Arrays.asList("actions",
                    "data",
                    "exclude-lambdas",
                    "ignored-bootstrap-packages",
                    "ignored-packages",
                    "internal",
                    "rules",
                    "scopes",
                    "special");

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void pastMap() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "scopes", "my-key");
            List<String> output = Arrays.asList("advanced",
                    "interfaces",
                    "methods",
                    "superclass",
                    "type");

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void pastList() {
            List<String> input = Arrays.asList("inspectit.");
            ArrayList<String> output = new ArrayList<>(Arrays.asList("config",
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

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void endsInWildcard() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex");
            ArrayList<String> output = new ArrayList<>();
            Type t = String.class;

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "data", "method_duration", "is-tag");
            ArrayList<String> output = new ArrayList<>();

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }

        @Test
        void classPropertyNotAdded() {
            List<String> input = Arrays.asList("inspectit.");

            assertThat(completer.getSuggestions(input)).doesNotContainSequence("class");
        }

        @Test
        void emptyString() {
            List<String> input = Arrays.asList("");
            ArrayList<String> output = new ArrayList<>();

            assertThat(completer.getSuggestions(input)).isEqualTo(output);
        }
    }

    @Nested
    public class GetProperties {
        @Test
        void getPropertiesInspectit() {
            List<String> output = Arrays.asList("config",
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

            assertThat(completer.getProperties(InspectitConfig.class)).isEqualTo(output);
        }
    }
}
