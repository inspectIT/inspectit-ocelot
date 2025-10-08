package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsAutoCompleterTest {

    @InjectMocks
    MetricsAutoCompleter metricsAutoCompleter;

    @Mock
    ConfigurationQueryHelper configurationQueryHelper;

    @Nested
    class GetSuggestions {

        @Test
        void wrongPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules");

            List<String> output = metricsAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }

        @Test
        void correctPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules", "test_rule", "metrics");
            List<String> mockOutput = Collections.singletonList("test_metric");
            when(configurationQueryHelper.getKeysForPath(any())).thenReturn(mockOutput);

            List<String> output = metricsAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(1);
            assertThat(output).contains("test_metric");
        }

        @Test
        void correctPathNothingDefined() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules", "test_rule", "metrics");

            List<String> output = metricsAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }
    }
}
