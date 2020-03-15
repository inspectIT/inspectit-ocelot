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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActionAutoCompleterTest {

    @InjectMocks
    ActionAutoCompleter actionAutoCompleter;

    @Mock
    ConfigurationQueryHelper configurationQueryHelper;

    @Nested
    public class GetSuggestions {
        @Test
        public void hasActions() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "actions");
            doReturn(Collections.singletonList("test"), Collections.singletonList("test2"), Collections.emptyList())
                    .when(configurationQueryHelper).getKeysForPath(any());

            List<String> output = actionAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(2);
            assertThat(output).contains("test");
            assertThat(output).contains("test2");
        }

        @Test
        public void noPaths() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "actions");
            when(configurationQueryHelper.getKeysForPath(any())).thenReturn(Collections.emptyList());

            List<String> output = actionAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }

        @Test
        public void wrongPath() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "rules");

            List<String> output = actionAutoCompleter.getSuggestions(path);

            assertThat(output).isEmpty();
        }

        @Test
        public void allOptionsChecked() {
            List<String> path = Arrays.asList("inspectit", "instrumentation", "actions");
            doReturn(Collections.singletonList("test")).when(configurationQueryHelper).getKeysForPath(any());

            List<String> output = actionAutoCompleter.getSuggestions(path);

            assertThat(output).hasSize(6);
            assertThat(output).containsOnly("test");
        }
    }
}
