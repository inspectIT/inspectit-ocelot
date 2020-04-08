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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ActionInputAutoCompleterTest {

    @InjectMocks
    ActionInputAutoCompleter actionInputAutoCompleter;

    @Mock
    ConfigurationQueryHelper configurationQueryHelper;

    @Nested
    public class GetSuggestions {
        @Test
        public void getSuggestionsTest() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "data-input");
            when(configurationQueryHelper.getKeysForPath(any())).thenReturn(Arrays.asList("test", "test2"));

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).hasSize(2);
            assertThat(output).contains("test");
            assertThat(output).contains("test2");
        }

        @Test
        public void wrongPath() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "data-input");

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).isEmpty();
        }

        @Test
        public void emptyLists() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "data-input");

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).isEmpty();
        }

        @Test
        public void ignoresUnderscoredValues() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "data-input");
            when(configurationQueryHelper.getKeysForPath(any())).thenReturn(Arrays.asList("test", "_test2"));

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).hasSize(1);
            assertThat(output).containsOnly("test");
        }
    }
}
