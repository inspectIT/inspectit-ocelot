package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.config.model.InspectitConfig;

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

            List<String> result = completer.getSuggestions(input);

            assertThat(result).containsExactlyInAnyOrder("actions", "data", "exclude-lambdas", "ignored-bootstrap-packages", "ignored-packages", "internal", "rules", "scopes", "special", "enabled");
        }

        @Test
        void pastMap() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "scopes", "my-key");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).containsExactlyInAnyOrder("docs", "advanced", "interfaces", "methods", "superclass", "type", "exclude");
        }

        @Test
        void pastList() {
            List<String> input = Arrays.asList("inspectit", "");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).containsExactlyInAnyOrder("config", "exporters", "instrumentation", "logging", "metrics", "plugins", "privacy", "publish-open-telemetry-to-bootstrap", "self-monitoring", "service-name", "tags", "thread-pool-size", "tracing", "agent-commands", "log-preloading");
        }

        @Test
        void endsInWildcard() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "actions", "string_replace_all", "input", "regex");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void endsInEnum() {
            List<String> input = Arrays.asList("inspectit", "tracing", "add-common-tags");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).hasSize(4);
            assertThat(result).contains("NEVER");
            assertThat(result).contains("ON_GLOBAL_ROOT");
            assertThat(result).contains("ON_LOCAL_ROOT");
            assertThat(result).contains("ALWAYS");
        }

        @Test
        void propertyIsPresentAndReadMethodIsNull() {
            List<String> input = Arrays.asList("inspectit", "instrumentation", "data", "method_duration", "is-tag");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void classPropertyNotAdded() {
            List<String> input = Arrays.asList("inspectit.");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void emptyString() {
            List<String> input = Arrays.asList("");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).containsExactlyInAnyOrder("inspectit");
        }

        @Test
        void startsLikeInspectit() {
            List<String> input = Arrays.asList("inspe");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void startsNotLikeInspectit() {
            List<String> input = Arrays.asList("test");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void startsNotWithInspectit() {
            List<String> input = Arrays.asList("test", "anotherTest");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }

        @Test
        void filtersInspectitEnvPath() {
            List<String> input = Collections.singletonList("inspectit");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).doesNotContain("env");
        }

        @Test
        void ignoresPastInspectitEnvPath() {
            List<String> input = Collections.singletonList("inspectit.env");

            List<String> result = completer.getSuggestions(input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    public class GetProperties {

        @Test
        void getPropertiesInspectit() {
            List<String> result = completer.getProperties(InspectitConfig.class);

            assertThat(result).containsExactlyInAnyOrder("config", "env", "exporters", "instrumentation", "logging", "metrics", "plugins", "privacy", "publish-open-telemetry-to-bootstrap", "self-monitoring", "service-name", "tags", "thread-pool-size", "tracing", "agent-commands", "log-preloading");
        }
    }
}
