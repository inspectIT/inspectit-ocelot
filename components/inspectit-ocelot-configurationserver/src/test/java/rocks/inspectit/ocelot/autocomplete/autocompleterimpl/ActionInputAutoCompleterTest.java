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
import static org.mockito.Mockito.doReturn;

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
            doReturn(Arrays.asList("action_A", "action_B")).when(configurationQueryHelper)
                    .getKeysForPath(eq(Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "action")));
            doReturn(Arrays.asList("first_input_of_A", "second_input_of_A")).when(configurationQueryHelper)
                    .getKeysForPath(eq(Arrays.asList("inspectit", "instrumentation", "actions", "action_A", "input")));
            doReturn(Arrays.asList("first_input_of_B", "second_input_of_B")).when(configurationQueryHelper)
                    .getKeysForPath(eq(Arrays.asList("inspectit", "instrumentation", "actions", "action_B", "input")));

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).hasSize(4);
            assertThat(output).contains("first_input_of_A");
            assertThat(output).contains("second_input_of_A");
            assertThat(output).contains("first_input_of_B");
            assertThat(output).contains("second_input_of_B");
        }

        @Test
        public void wrongPath() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "scopes", "my_scopes");

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).isEmpty();
        }

        @Test
        public void ignoresUnderscoredValues() {
            List<String> testPath = Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "data-input");
            doReturn(Arrays.asList("action_A", "action_B")).when(configurationQueryHelper)
                    .getKeysForPath(eq(Arrays.asList("inspectit", "instrumentation", "rules", "my_method", "entry", "span_name", "action")));
            doReturn(Arrays.asList("first_input_of_A", "_second_input_of_A")).when(configurationQueryHelper)
                    .getKeysForPath(eq(Arrays.asList("inspectit", "instrumentation", "actions", "action_A", "input")));

            List<String> output = actionInputAutoCompleter.getSuggestions(testPath);

            assertThat(output).hasSize(1);
            assertThat(output).containsOnly("first_input_of_A");
        }
    }
}
