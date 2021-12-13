package rocks.inspectit.ocelot.autocomplete.autocompleterimpl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.autocomplete.util.ConfigurationQueryHelper;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RuleAutoCompleterTest {

    @InjectMocks
    RuleAutoCompleter ruleAutoCompleter;

    @Mock
    ConfigurationQueryHelper configurationQueryHelper;


    @Nested
    public class GetSuggestions {
        @Test
        public void wrongPath() {
            List<String> path = Arrays.asList("inspectit", "metrics", "processor");

            List<String> output = ruleAutoCompleter.getSuggestions(path);

            verifyNoMoreInteractions(configurationQueryHelper);
            assertThat(output).hasSize(0);
        }

        @Test
        public void correctPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules");
            List<String> mockOutput = Arrays.asList("my_rule", "another_rule");
            when(configurationQueryHelper.getKeysForPath(eq(path))).thenReturn(mockOutput);

            List<String> output = ruleAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(2);
            assertThat(output).contains("my_rule");
            assertThat(output).contains("another_rule");
        }

        @Test
        public void correctPathNothingDefined() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules");

            List<String> output = ruleAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(0);
        }
    }
}
